function [k, plist] = evalrankMOga(D,S)
% x - the first listed permutation vector that mimizes k
% k - the corresponding minimized k
% exitflag - an integer identifying the reason the alg terminated
% output - a structure that contains output from each generation and
% performance
% population - matrix whose rows are the final permutation vectors left
% scores - matrix with corresponding k values for the permutation vectors


nvars = length(D);
options = optimoptions('gamultiobj', 'CrossoverFcn',@rankxover,...
    'MutationFcn',@rankmutation, 'CreationFcn',@rankcreationfcn);

[x,k,exitflag,output,population,scores] = gamultiobj(@(perm) (rankmultifitness(perm,D,S)),nvars,[],[],[],[],[],[],options);
%The below uses find to list the indexes of the scores that give the fval
%(kmin). This is used to pull out the corresponding permutation in the
%population. Then, since many of these are actually the same permutation,
%unique() pull out the unique permutations giving kmin. This is p.
plist = unique(population(find(scores(:)==k),:),'rows');
end