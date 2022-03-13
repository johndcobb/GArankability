function fitness = rankfitness(perm,D)
%fitness (k) is found by adding together how many "corrections" would be needed
%to turn the matrix into the perfectly ranked situation - strong dominance.
perfectRG=triu(ones(size(D,1)),1);
fitness=sum(sum(abs(perfectRG-D(perm,perm))));
end
