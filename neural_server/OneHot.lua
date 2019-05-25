
local OneHot, parent = torch.class('OneHot', 'nn.Module')

function OneHot:__init(outputSize)
  parent.__init(self)
  self.outputSize = outputSize
  -- We'll construct one-hot encodings by using the index method to
  -- reshuffle the rows of an identity matrix. To avoid recreating
  -- it every iteration we'll cache it.
  self._eye = torch.eye(outputSize)
end

function OneHot:updateOutput(input)
  local inputFlatten = input:view(-1)
  self.output:resize(inputFlatten:size(1), self.outputSize):zero()
  if self._eye == nil then self._eye = torch.eye(self.outputSize) end
  self._eye = self._eye:float()
  local longInput = inputFlatten:long()
  self.output:copy(self._eye:index(1, longInput))
  if input:dim() == 2 then
    self.output = self.output:view(input:size(1), input:size(2), self.outputSize)
  end
  return self.output
end
