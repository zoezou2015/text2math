local utf8 = require 'lua-utf8'

local opt = {}
--file = torch.DiskFile('/home/rotmanmi/Data/Polyglot/Polyglot.twitter.27B.25d.txt','r')
--file = torch.DiskFile(opt.binfilename,'r')

opt.binfilename = arg[1]
opt.outfilename = arg[2]

--Reading Header

local encodingsize = -1
local ctr = 0
for line in io.lines(opt.binfilename) do
    if ctr == 0 then
        for i in utf8.gmatch(line, "%S+") do
            encodingsize = encodingsize + 1
        end
    end
    ctr = ctr + 1

end

words = ctr
size = encodingsize




local w2vvocab = {}
local v2wvocab = {}
local M = torch.FloatTensor(words,size)

--Reading Contents
isGerman = string.find(opt.binfilename,"-de")

i = 1
for line in io.lines(opt.binfilename) do
    xlua.progress(i,words)
    local vecrep = {}
    for i in utf8.gmatch(line, "%S+") do
        table.insert(vecrep, i)
    end
    str = vecrep[1]
    table.remove(vecrep,1)
	vecrep = torch.FloatTensor(vecrep)

	local norm = torch.norm(vecrep,2)
	if norm ~= 0 then vecrep:div(norm) end

    -- lowercase
    strlow = str
    if str ~= "<S>" and str ~= "</S>" and str ~= "<PAD>" and str ~= "<UNK>" then
        strlow = utf8.lower(str)
    end
    if isGerman then
        strlow = utf8.gsub(strlow, "ä", "ae")
        strlow = utf8.gsub(strlow, "ö", "oe")
        strlow = utf8.gsub(strlow, "ü", "ue")
        strlow = utf8.gsub(strlow, "ß", "ss")
    end
    if w2vvocab[strlow] == nil then
    	w2vvocab[strlow] = i
    	v2wvocab[i] = strlow
    	M[{{i},{}}] = vecrep
        i = i + 1
    end
end


--Writing Files
Polyglot = {}
Polyglot.M = M
Polyglot.w2vvocab = w2vvocab
Polyglot.v2wvocab = v2wvocab
torch.save(opt.outfilename,Polyglot)
print('Writing t7 File for future usage.')



return Polyglot


