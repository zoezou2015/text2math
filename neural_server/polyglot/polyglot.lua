torch.setdefaulttensortype('torch.FloatTensor')

-- opt = {
--     binfilename = 'polyglot/polyglot-en.txt',
--     outfilename = 'polyglot/polyglot-en.t7'
-- }
local Polyglot = {}
-- if not paths.filep(opt.outfilename) then
-- 	Polyglot = require('bintot7.lua')
-- else
-- 	Polyglot = torch.load(opt.outfilename)
-- 	print('Done reading Polyglot data.')
-- end

Polyglot.load = function (self,lang)
    local polyglotFile = 'polyglot/polyglot-' .. lang .. '.t7'
    if not paths.filep(polyglotFile) then
        error('Please run bintot7.lua to preprocess Polyglot data!')
    else
        Polyglot.polyglot = torch.load(polyglotFile)
        print('Done reading Polyglot data.')
    end
end

Polyglot.distance = function (self,vec,k)
	local k = k or 1	
	--self.zeros = self.zeros or torch.zeros(self.M:size(1));
	local norm = vec:norm(2)
	vec:div(norm)
	local distances = torch.mv(self.polyglot.M ,vec)
	distances , oldindex = torch.sort(distances,1,true)
	local returnwords = {}
	local returndistances = {}
	for i = 1,k do
		table.insert(returnwords, self.polyglot.v2wvocab[oldindex[i]])
		table.insert(returndistances, distances[i])
	end
	return {returndistances, returnwords}
end

Polyglot.word2vec = function (self,word,throwerror)
   local throwerror = throwerror or false
   local ind = self.polyglot.w2vvocab[word]
   if throwerror then
		assert(ind ~= nil, 'Word does not exist in the dictionary!')
   end
	if ind == nil then
        print('unknown: ' .. word)
		ind = self.polyglot.w2vvocab['<UNK>']
	end
   return self.polyglot.M[ind]
end

return Polyglot
