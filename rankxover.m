function xoverKids = rankxover(parents, options, nvars, FitnessFcn, ...
    unused,thisPopulation)
%parents - Row vector of parents chosen by the selection function
%options - options structure
%nvars - Number of variables
%FitnessFcn - Fitness function
%unused - Placeholder not used
%thisPopulation - Matrix representing the current population. The number of rows of the matrix is Population size and the number of columns is Number of variables.
%%%%%%%%%%%%%%%%%%
xoverKids = zeros(length(parents)/2,nvars);
%NEED TO DETERMINE IF LENGTH OF PARENTS IS EVEN. --- YES

for i=1:2:length(parents)
    parentPair={thisPopulation(parents(i),:), thisPopulation(parents(i+1),:)};
    choiceparent = randsample(parentPair,1);
    xoverKids(ceil(i/2),:) = choiceparent{1};
    chooseTransposition = randsample(choiceparent{1},2);
    xoverKids(ceil(i/2),chooseTransposition) = xoverKids(ceil(i/2),chooseTransposition([2,1]));
end
end

%The code block below can be substituted above to make the children differ only by
%transpositions that are directly next to each other. I did this after
%observing that many p are very close to each other. This may be offbase,
%it made runtime WAY longer
%for i=1:2:length(parents)
%    parentPair={thisPopulation(parents(i),:), thisPopulation(parents(i+1),:)};
%    choiceparent = randsample(parentPair,1);
%    xoverKids(ceil(i/2),:) = choiceparent{1};
%    chooseTransposition = randsample(choiceparent{1},1);
%    xoverKids(ceil(i/2),[chooseTransposition mod(chooseTransposition+1,nvars)+1]) = xoverKids(ceil(i/2),[mod(chooseTransposition+1,nvars)+1 chooseTransposition]);
%end
%end
