require "nn"
require "OneHot"
require 'optim'

local mp = require 'MessagePack'

cmd = torch.CmdLine()
cmd:text()
cmd:text('Neural Network Server')
cmd:text()
cmd:option('-port', 5556, 'port number')
cmd:option('-gpuid', -1, 'which GPU to use (>= 0, -1 = CPU)')
cmd:text()
opt = cmd:parse(arg)
portNumber = opt.port
print("listening on port " .. opt.port)

local zmq = require "lzmq"
local context = zmq.init(1)

SEED = 1337
torch.manualSeed(SEED)

-- GPU setup
gpuid = opt.gpuid
if gpuid >= 0 then
    local ok, cunn = pcall(require, 'cunn')
    local ok2, cutorch = pcall(require, 'cutorch')
    if not ok then print('package cunn not found!') end
    if not ok2 then print('package cutorch not found!') end
    if ok and ok2 then
        print('using CUDA on GPU ' .. gpuid .. '...')
        cutorch.setDevice(gpuid + 1) -- note +1 to make it 0 indexed! sigh lua
        cutorch.manualSeed(SEED)
    else
        print('If cutorch and cunn are installed, your CUDA toolkit may be improperly configured.')
        print('Check your CUDA toolkit installation, rebuild cutorch and cunn, and try again.')
        print('Falling back on CPU mode')
        gpuid = -1 -- overwrite user setting
    end
else
    print("CPU mode")
end

local socket = context:socket(zmq.REP)
socket:bind("tcp://*:" .. portNumber)

local ret -- return value to client
local inputLayer
local mlp -- our neural net
local params, gradParams -- mlp's params
local x, fixEmbedding -- input to nn, which is fixed
local wordList, word2idx
local numInput
local outputDim
local doOptimization, optimizer, optimState

local polyglot
function loadPolyglot(wordList, lang)
    if polyglot == nil then
        polyglot = require 'polyglot/polyglot'
    end
    polyglot:load(lang)
    ltw = nn.LookupTable(#wordList, 64)
    for i=1,#wordList do
        local emb = torch.Tensor(64)
        local p_emb = polyglot:word2vec(wordList[i])
        for j=1,64 do
            emb[j] = p_emb[j]
        end
        ltw.weight[i] = emb
    end
    return ltw
end

function init_MLP(data)
    -- numInputList, inputDimList, embSizeList, outputDim,
    -- numLayer, hiddenSize, activation, dropout
    -- vocab

    -- re-seed
    torch.manualSeed(SEED)
    if gpuid >= 0 then cutorch.manualSeed(SEED) end

    -- what to forward
    x = prepare_input(data.vocab, data.numInputList, data.embSizeList)
    fixEmbedding = data.fixEmbedding
    wordList = data.wordList
    word2idx = {}
    for i=1,#wordList do
        word2idx[wordList[i]] = i
    end
    numInput = x[1]:size(1)

    -- input layer
    local pt = nn.ParallelTable()
    local totalInput = 0
    local totalDim = 0
    for i=1,#data.inputDimList do
        local inputDim = data.inputDimList[i]
        local lt
        if data.embSizeList[i] == 0 then
            lt = OneHot(inputDim)
            totalDim = totalDim + data.numInputList[i] * inputDim
        else
            if data.embedding ~= nil then
                if data.embedding[i] == 'polyglot' then
                    lt = loadPolyglot(data.wordList, data.lang)
                else -- unknown/no embedding, defaults to random init
                    lt = nn.LookupTable(inputDim, data.embSizeList[i])
                end
            else
                lt = nn.LookupTable(inputDim, data.embSizeList[i])
            end
            totalDim = totalDim + data.numInputList[i] * data.embSizeList[i]
        end
        if data.fixEmbedding then
            lt.accGradParameters = function() end
        end
        pt:add(nn.Sequential():add(lt):add(nn.View(numInput,-1)))
        totalInput = totalInput + data.numInputList[i]
    end

    local jt = nn.JoinTable(2)

    mlp = nn.Sequential()
    if data.fixEmbedding then
        inputLayer = nn.Sequential()
        inputLayer:add(pt)
        inputLayer:add(jt)
    else
        mlp:add(pt)
        mlp:add(jt)
    end

    -- hidden layer
    for i=1,data.numLayer do
        if data.dropout ~= nil and data.dropout > 0 then
            mlp:add(nn.Dropout(data.dropout))
        end

        local ll
        if i == 1 then
            ll = nn.Linear(totalDim, data.hiddenSize)
        else
            ll = nn.Linear(data.hiddenSize, data.hiddenSize)
        end
        mlp:add(ll)

        local act
        if data.activation == nil or data.activation == "relu" then
            act = nn.ReLU()
        elseif data.activation == "tanh" then
            act = nn.Tanh()
        elseif data.activation == "hardtanh" then
            act = nn.HardTanh()
        elseif data.activation == "identity" then
            -- do nothing
        else
            error("activation " .. activation .. " not supported")
        end
        if act ~= nil then
            mlp:add(act)
        end
    end

    if data.dropout ~= nil and data.dropout > 0 then
        mlp:add(nn.Dropout(data.dropout))
    end

    -- output layer (passed to CRF)
    outputDim = data.outputDim
    local lastInputDim
    if data.numLayer == 0 then
        lastInputDim = totalDim
    else
        lastInputDim = data.hiddenSize
    end
    -- mlp:add(nn.Linear(lastInputDim, outputDim):noBias()) -- no bias
    mlp:add(nn.Linear(lastInputDim, outputDim))
    if gpuid >= 0 then
        if data.fixEmbedding then inputLayer:cuda() end
        mlp:cuda()
    end

    -- set optimizer. If nil, optimization is done by caller.
    doOptimization = data.optimizer ~= nil and data.optimizer ~= 'none'
    if data.optimizer == 'sgd' then
        optimizer = optim.sgd
        optimState = {learningRate=data.learningRate}
    elseif data.optimizer == 'adagrad' then
        optimizer = optim.adagrad
        optimState = {learningRate=data.learningRate}
    elseif data.optimizer == 'adam' then
        optimizer = optim.adam
        optimState = {learningRate=data.learningRate}
    end

    params, gradParams = mlp:getParameters()
end

function fwd_MLP(mlp, x, newParams, training)
    if training == true then
        mlp:training()
    else
        mlp:evaluate()
    end

    if newParams ~= nil then
        params:copy(newParams)
    end

    local input_x = x

    if fixEmbedding then
        input_x = inputLayer:forward(x)
    end

    return mlp:forward(input_x)
end

function feval(params) -- for optim
    return 0, gradParams
end

function bwd_MLP(mlp, x, gradOutput)
    gradParams:zero()

    local input_x = x

    if fixEmbedding then
        input_x = inputLayer:forward(x)
    end

    mlp:backward(input_x, gradOutput)
    
    if doOptimization then
        optimizer(feval, params, optimState)
    end
end

function serialize(data)
    local timer = torch.Timer()
    local ret = data:view(-1):totable()
    local time = timer:time().real
    print(string.format("Serializing took %.4fs", time))
    return ret
end

function deserialize(data, row, col)
    local timer = torch.Timer()
    local ret
    if row == 1 then
        ret = torch.Tensor(data)
    else
        ret = torch.Tensor(data):view(row, col)
    end
    if gpuid >= 0 then ret = ret:cuda() end
    local time = timer:time().real
    print(string.format("Deserializing took %.4fs", time))
    return ret
end

function save_model(prefix)
    local obj = {}
    obj["mlp"] = mlp
    obj["word2idx"] = word2idx
    torch.save("model/" .. prefix .. ".t7",obj)
end

function load_model(prefix)
    local saved_obj = torch.load("model/" .. prefix .. ".t7")
    local saved_mlp = saved_obj.mlp
    local saved_word2idx = saved_obj.word2idx
    local saved_lt = saved_mlp:get(1):get(1):get(1)
    local orig_lt = mlp:get(1):get(1):get(1)
    for i=1,#wordList do
        saved_word_idx = saved_word2idx[wordList[i]]
        if saved_word_idx ~= nil then
            orig_lt.weight[i]:copy(saved_lt.weight[saved_word_idx])
        end
    end
    saved_lt.weight = orig_lt.weight
    mlp = saved_mlp
    mlp:get(1):get(1):get(2):__init(numInput,-1)
end

function prepare_input(vocab, numInputList, embSizeList)
    local result = {}
    local startIdx = 0
    for i=1,#numInputList do
        table.insert(result, torch.Tensor(#vocab, numInputList[i]))
        for j=1,#vocab do
            for k=1,numInputList[i] do
                result[i][j][k] = vocab[j][startIdx+k]
            end
        end
        startIdx = startIdx + numInputList[i]
        if gpuid >= 0 then result[i] = result[i]:cuda() end
    end
    return result
end

while true do
    --  Wait for next request from client
    local request = socket:recv()
    if request ~= nil then
        request = mp.unpack(request)
        if request.cmd == "init" then
            timer = torch.Timer()
            init_MLP(request)
            if request.fixEmbedding then print(inputLayer) end
            print(mlp)
            if doOptimization then -- no return array if optim is done here
                ret = 1
            else
                ret = serialize(params)
            end
            time = timer:time().real
            print(string.format("Init took %.4fs", time))
        elseif request.cmd == "fwd" then
            local timer = torch.Timer()
            local newParams
            if not doOptimization then
                newParams = deserialize(request.weights, 1, -1)
            end
            local fwd_out = fwd_MLP(mlp, x, newParams, request.training)
            ret = serialize(fwd_out)
            time = timer:time().real
            print(string.format("Forward took %.4fs", time))
        elseif request.cmd == "bwd" then
            local timer = torch.Timer()
            local gradOut = deserialize(request.grad, numInput, outputDim)
            bwd_MLP(mlp, x, gradOut)
            if doOptimization then
                ret = 1
            else
                ret = serialize(gradParams)
            end
            time = timer:time().real
            print(string.format("Backward took %.4fs", time))
        elseif request.cmd == "save" then
            local timer = torch.Timer()
            save_model(request.savePrefix)
            time = timer:time().real
            print(string.format("Saving model took %.4fs", time))
            ret = 1
        elseif request.cmd == "load" then
            local timer = torch.Timer()
            load_model(request.savePrefix)
            time = timer:time().real
            print(string.format("Loading model took %.4fs", time))
            ret = 1
        end
        ret = mp.pack(ret)
        socket:send(ret)
    end
end
--  We never get here but if we did, this would be how we end
socket:close()
context:term()
