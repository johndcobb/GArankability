function[Ms] = permMatrices(m)%Can be made distributed cell
Ms = cell(1,factorial(m));
C = nchoosek(0:m:m*(m-1),m);
   P = perms(1:m);
     for ip = 1:size(P,1)
       M = zeros(m);
       M(P(ip,:)+C(1,:)) = 1;
       Ms{ip}=M;% Do whatever you want to do with M
     end
end