function [k,p] = ga3(D)

prevp = -1;
p = 0;
k = Inf;
maxp = 10;
while p ~= prevp && maxp <= 80
    prevp = p;
    nvars = size(D,1)*maxp;
    options = optimoptions('gamultiobj','UseVectorized',false);
    [x,fval,exitflag,output,population,scores] = gamultiobj(@(permP) (rankability_fitness3(permP,maxp,D)),nvars,[],[],[],[],zeros(1,nvars),ones(1,nvars),options);
    fval(:,2) = -1*fval(:,2); % switch back so we are maximizing p
    
    mink = min(fval(:,1));
    if mink <= k
        minkinxs = find(fval(:,1) == mink);
        % Within those, pick out the one with the maximum number for p (min of the
        % ojective)
        maxPobj = max(fval(minkinxs,2));
        % Find all those with the maximum value of P
        maxPinxs = find(fval(minkinxs,2) == maxPobj);    
        inx = minkinxs(maxPinxs(1));
        [y,fitness,perms,unique_rows] = rankability_fitness2(x(inx,:),maxp,D);
        % Just make sure we didn't go down in p but stay the same value of
        % k
        if k == mink && size(unique_rows,1) < p
            display('Solution got worse');
            break
        end
        p = size(unique_rows,1);
        k = mink;
    end
    maxp = maxp*2 % double for next time
end
fprintf('maxp: %d\n',maxp);