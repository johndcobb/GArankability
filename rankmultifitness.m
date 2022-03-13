function [job1fitness,job2fitness] = rankmultifitness(perm,D,S)
%This function will perform two jobs and return the results from each in a
%vector for evalrankMOga. These jobs are as follows:
%1) assess the (k) of the given permutation
%on D, our input matrix. (identical to rankfitness)
%2) Minimize the sum of kendall tau coefficients between perm and each of
%the plist solution sets (In S as a cell array) from previous runs. When kendall tau is minimial,
%perm is unlike the previous solutions.

perfectRG=triu(ones(size(D,1)),1);
job1fitness=sum(sum(abs(perfectRG-D(perm,perm))));

num_solnsets = size(S,2);
job2fitness = 0;
%for i=1:num_solnsets
    % Kendall tau computes columnwise, so we need the transpose
%    solnset = transpose(S{i});
%    setRho = corr(solnset,transpose(perm),'type','Kendall');
%    job2fitness = job2fitness + sum(setRho);
%end

%This is a modification to the previous fitness function that assumes that
%all previous solns are merged into one set in S{1}
if ~(num_solnsets==0)
solnset = transpose(S{1});
rho = corr(solnset, transpose(perm),'type', 'Kendall');
job2fitness = sum(rho);
end
end




