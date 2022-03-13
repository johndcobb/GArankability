function Population = rankcreationfcn(Genomelength, FitnessFcn, options)
% n! must be above 200 i.e. n>5
wholePop = perms(1:Genomelength);
Population = wholePop(randsample(size(wholePop,1),200),:);
end
