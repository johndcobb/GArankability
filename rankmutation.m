function mutationChildren = rankmutation(parents, options, nvars, FitnessFcn,...
    state, thisScore, thisPopulation)
%The arguments to the function are
%parents ? Row vector of parents chosen by the selection function
%options ? Options structure
%nvars ? Number of variables
%FitnessFcn ? Fitness function
%state ? Structure containing information about the current generation. The State Structure describes the fields of state.
%thisScore ? Vector of scores of the current population
%thisPopulation ? Matrix of individuals in the current population
%%%%%%%%%%%%%%%%%%%%%%%%
mutationChildren = zeros(length(parents),nvars);
%choose a tranposition and make it for each parent
for i=1:length(parents)
    pparent=thisPopulation(parents(i),:);
    mutationChildren(i, :) = pparent;
    chooseTransposition = randsample(pparent,2);
    mutationChildren(i,chooseTransposition) = pparent([chooseTransposition(2) chooseTransposition(1)]); %Perform transposition
end
end