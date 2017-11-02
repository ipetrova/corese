package fr.inria.corese.triple.function.core;

import fr.inria.acacia.corese.api.Computer;
import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.exceptions.CoreseDatatypeException;
import fr.inria.acacia.corese.triple.parser.Expression;
import fr.inria.corese.triple.function.term.Binding;
import fr.inria.corese.triple.function.term.TermEval;
import fr.inria.edelweiss.kgram.api.query.Environment;
import fr.inria.edelweiss.kgram.api.query.Producer;
import java.util.ArrayList;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class IfThenElseTerm extends TermEval {
    Expression test, e1, e2;
      
    public IfThenElseTerm(String name){
        super(name);
    }
    
    @Override
    public void add(Expression exp) {
        super.add(exp);
        if (getArgs().size() == 3) {
            init();
        }
    }
    
    @Override
    public void setArg(int i, Expression exp){
        super.setArg(i, exp);
        set(i, exp);
    }
    
    @Override
    public void setArgs(ArrayList<Expression> list) {
        super.setArgs(list);
        init();
    }
    
    void init() {
        for (int i = 0; i < getArgs().size(); i++) {
            set(i, getArg(i));
        }
    }
    
    void set(int i, Expression exp){
        switch (i) {
            case 0: test = exp; break;
            case 1: e1 = exp; break;
            case 2: e2 = exp; break;
        }
    }
       
     @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) {
        IDatatype val = test.eval(eval, b, env, p);
        if (val == null) {
            return null;
        }
        if (val.booleanValue()) {
            return e1.eval(eval, b, env, p);
        } else {
            return e2.eval(eval, b, env, p);
        }
    }
    
    
    public IDatatype eval2(Computer eval, Binding b, Environment env, Producer p) {
        IDatatype val = test.eval(eval, b, env, p);
        if (val == null) {
            return null;
        }
        try {
            if (val.isTrue()) {
                return e1.eval(eval, b, env, p);
            } 
        } catch (CoreseDatatypeException e) {
            // continue
        }
        return e2.eval(eval, b, env, p);
    }   
   
}
