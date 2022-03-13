D=[0 1 0 1 0 0; 0 0 0 0 0 0; 0 0 0 1 0 1; 0 1 0 0 0 0; 0 0 0 1 0 0; 0 1 0 0 1 0];
nvars = length(D);
options = optimoptions('ga', 'CrossoverFcn',@rankxover,...
    'MutationFcn',@rankmutation, 'CreationFcn',@rankcreationfcn);
[x,fval,exitflag,output,population,scores] = ga(@(perm) (rankfitness(perm,D)),nvars,options);
x
fval

%The below uses find to list the indexes of the scores that give the fval
%(kmin). This is used to pull out the corresponding permutation in the
%population. Then, since many of these are actually the same permutation,
%unique() pull out the unique permutations giving kmin. This is p.
p = unique(population(find(scores(:)==fval),:),'rows')