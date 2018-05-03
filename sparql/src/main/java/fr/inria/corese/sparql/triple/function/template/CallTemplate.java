package fr.inria.corese.sparql.triple.function.template;

import fr.inria.corese.sparql.api.Computer;
import fr.inria.corese.sparql.api.IDatatype;
import fr.inria.corese.sparql.triple.function.term.Binding;
import fr.inria.corese.kgram.api.query.Environment;
import fr.inria.corese.kgram.api.query.Producer;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class CallTemplate extends TemplateFunction {  
        
    public CallTemplate(String name){
        super(name);
    }
    
    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) {
        IDatatype[] param = evalArguments(eval, b, env, p, 0);
        if (param == null){
            return null;
        }
        switch (param.length){
            case 1:  return eval.getComputerTransform().transform(null, param[0], null, this, env, p); 
            case 2:  return eval.getComputerTransform().transform(null, param[1], null, param[0], null, this, env, p); 
            default: return eval.getComputerTransform().transform(getParam(param, 1), param[1], null, param[0], null, this, env, p); 
        }
    }
      
}
