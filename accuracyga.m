%Accuracy test for my GA
X = condigraphs(6,10);
numtests = size(X,3);
k1list = zeros(1,numtests);
k2list = zeros(1,numtests);
p1list = zeros(1,numtests);
p2list = zeros(1,numtests);
for i=1:numtests
    [k1list(i),plist]=evalrankga(X(:,:,i));
    p1list(i) = size(plist,1);
    [k2list(i),p2list(i)] =brute_force(X(:,:,i));
end