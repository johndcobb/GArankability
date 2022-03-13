function[k]= brutefindk(X)
perfectRG=triu(ones(size(X,1)),1);
k=sum(sum(abs(perfectRG-X)));
