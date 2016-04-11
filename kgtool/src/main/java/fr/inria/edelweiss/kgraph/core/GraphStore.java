package fr.inria.edelweiss.kgraph.core;

import fr.inria.edelweiss.kgram.api.core.ExpType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

/**
 * Draft Graph Store where named graphs are hidden for system use 
 * 
 * @author Olivier Corby, Wimmics Inria I3S, 2013
 *
 */
public class GraphStore extends Graph {

   

    private HashMap<String, Graph> store;


    GraphStore() {
        store = new HashMap<String, Graph>();
    }

    public static GraphStore create() {
        return new GraphStore();
    }

    public static GraphStore create(boolean b) {
        GraphStore gs = new GraphStore();
        if (b) {
            gs.setEntailment();
        }
        return gs;
    }
    
    @Override
     public GraphStore copy(){
        GraphStore g = new GraphStore();
        g.inherit(this);
        g.copy(this);
        g.setNamedGraph(this);
        return g;
    }

    void setNamedGraph(GraphStore g) {
        for (String name : g.getNames()) {
            setNamedGraph(name, g.getNamedGraph(name));
        }
    }

    @Override
    public Graph getNamedGraph(String name) {
        return getStore().get(name);
    }

    public Graph createNamedGraph(String name) {
        Graph g = Graph.create();
        g.index();
        getStore().put(name, g);
        return g;
    }
    
    public Graph getCreateNamedGraph(String name) {
        Graph g = getNamedGraph(name);
        if (g != null){
            return g;
        }
        g = createNamedGraph(name);
        return g;
    }

    @Override
    public void setNamedGraph(String name, Graph g) {
        getStore().put(name, g);
    }

    public Collection<Graph> getNamedGraphs() {
        return getStore().values();
    }
    
    public Set<String> getNames(){
        return getStore().keySet();
    }

    public Graph getDefaultGraph() {
        return this;
    }

    /**
     * @return the store
     */
    public HashMap<String, Graph> getStore() {
        return store;
    }

    /**
     * @param store the store to set
     */
    public void setStore(HashMap<String, Graph> store) {
        this.store = store;
    }
    
   
    
   
    
    
    
    
}
