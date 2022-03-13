function[kmin,p] = solveKP(X)
n=size(X,1);
permats = permMatrices(size(X,1));
klist = zeros(1,factorial(n));
for i = 1:factorial(n)
    klist(i) = brutefindk(permats{i}*X*transpose(permats{i}));
end
kmin = min(klist);
p = sum(klist==kmin);
    