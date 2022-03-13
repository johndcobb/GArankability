function [y,fitness,perms,unique_rows] = rankability_fitness2(perm_real_P,maxP,D)
    perm_real_P_reshaped = reshape(perm_real_P,maxP,size(D,1));
    fitness = zeros(1,maxP);
    perms = zeros(size(perm_real_P_reshaped));
    for i = 1:maxP
        perm_real = perm_real_P_reshaped(i,:);
        [vs,perm] = sort(perm_real);
        perms(i,:) = perm;
        n=size(D,1); % n=number of items to be ranked, e.g., number of teams
        fitness(i)=nnz(tril(D(perm,perm)))+(n*(n-1)/2 - nnz(triu(D(perm,perm))));
    end
    
    % obj1 is simply the minimum fitness
    obj1 = min(fitness);
    % obj2 is how many unique ways we've found to accomplish this task
    % (negative so we can minimize this)
    unique_rows = unique(perms(fitness == obj1,:),'rows');
    obj2 = -size(unique_rows,1);
    
    y = [obj1,obj2];
end