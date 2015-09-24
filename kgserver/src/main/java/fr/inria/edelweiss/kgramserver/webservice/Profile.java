package fr.inria.edelweiss.kgramserver.webservice;

import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.cg.datatype.DatatypeMap;
import fr.inria.acacia.corese.exceptions.EngineException;
import fr.inria.acacia.corese.triple.parser.Context;
import fr.inria.acacia.corese.triple.parser.NSManager;
import fr.inria.edelweiss.kgram.api.core.Edge;
import fr.inria.edelweiss.kgram.api.core.Entity;
import fr.inria.edelweiss.kgram.api.core.Node;
import fr.inria.edelweiss.kgram.core.Mapping;
import fr.inria.edelweiss.kgram.core.Mappings;
import static fr.inria.edelweiss.kgramserver.webservice.EmbeddedJettyServer.port;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgraph.core.GraphStore;
import fr.inria.edelweiss.kgraph.query.QueryProcess;
import fr.inria.edelweiss.kgtool.load.Load;
import fr.inria.edelweiss.kgtool.load.LoadException;
import fr.inria.edelweiss.kgtool.load.QueryLoad;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parser of RDF profile that defines: 1- a set of profile (eg specifying a
 * Transformation) 2- a set of server with specific data to be loaded in
 * dedicated TripleStore profile is used with profile argument:
 * /template?profile=st:sparql server is used by Tutorial service that manage
 * specific TripleStores each server specify the RDF content of a TripleStore
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2014
 *
 */
public class Profile {

    static final String NL = System.getProperty("line.separator");
    static  String SERVER  = "http://localhost:" + EmbeddedJettyServer.port;
    static  String DATA = SERVER + "/data/";
    static  String QUERY = DATA + "query/";
      
    HashMap<String,  Service> map, servers; 
    NSManager nsm;
    IDatatype profile;

    boolean isProtected = false;

    static {
        try {
            SERVER = "http://" + InetAddress.getLocalHost().getCanonicalHostName();
            SERVER += (port == 80) ? "" : ":" + port;
            DATA = SERVER + "/data/";
            QUERY = DATA + "query/";
        } catch (UnknownHostException ex) {
            Logger.getLogger(Profile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    Profile() {
        this(false);
    }

    Profile(boolean b) {
        map = new HashMap();
        servers = new HashMap();
        nsm = NSManager.create();
        isProtected = b;
    }

    void setProtect(boolean b) {
        isProtected = b;
    }

    boolean isProtected() {
        return isProtected;
    }

    /**
     * Complete service parameters according to a profile e.g. get
     * transformation from profile
     */
    Param complete(Param par) throws IOException {
        String value = par.getValue();
        String query = par.getQuery();
        String name = par.getName();
        String transform = par.getTransform();
        String profile = par.getProfile();
        String uri = par.getUri();

        if (profile != null) {
            // prodile declare a construct where query followed by a transformation
            String uprofile = nsm.toNamespace(profile);
            // may load a new profile            
            define(uprofile);
            Service s = getService(uprofile);
            if (s != null) {
                if (name == null) {
                    // parameter name overload profile name
                    name = s.getQuery();
                }
                if (transform == null) {
                    // transform parameter overload profile transform
                    transform = s.getTransform();
                }               
                if (uri != null) {
                    // resource given as a binding value to the query
                    // generate values clause if profile specify variable
                    value = getValues(s.getVariable(), uri);
                }                     
                if (s.getParam() != null){
                    par.setContext(s.getParam().copy());
                }
            }
        }
       
        if (query == null){ 
            if (name != null){
            // load query definition
                query = loadQuery(name);
            }
        }
        else if (isProtected) {
            par.setUserQuery(true);
        }

        if (value != null && query != null) {
            // additional values clause
            query += value;
        }

        par.setTransform(transform);
        par.setName(name);
        par.setQuery(query);       
        return par;

    }

    public Collection<Service> getServices() {
        return map.values();
    }

    public Collection<Service> getServers() {
        return servers.values();
    }

    void define(String name) {
        if (!map.containsKey(name) && !isProtected) {
            //init(WEBAPP_DATA, name);
            System.out.println("Profile: " + name);
            init(name);
        }
    }

    void process(Graph g) throws IOException, EngineException {
        init(g);
        initLoad(g);
        initLoader(g);
    }

    void initServer(String name) {
        init(DATA + name);
    }
    
    void setProfile(Graph g){
        if (profile == null){
            profile = DatatypeMap.createObject(Context.STL_SERVER_PROFILE, g);
        }
    }
    
    IDatatype getProfile(){
        return profile;
    }

    /**
     *
     * path = DATA + profile.ttl
     */
    void init(String path) {
        try {
            Graph g = load(path);
            setProfile(g);
            process(g);
        } catch (IOException ex) {
            Logger.getLogger(Profile.class.getName()).log(Level.SEVERE, null, ex);
        } catch (LoadException ex) {
            Logger.getLogger(Profile.class.getName()).log(Level.SEVERE, null, ex);
        } catch (EngineException ex) {
            Logger.getLogger(Profile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    GraphStore loadServer(String name) throws IOException, LoadException {
        return load(DATA + name);
    }

    GraphStore load(String path) throws IOException, LoadException {
        GraphStore g = GraphStore.create();
        Load load = Load.create(g);
        load.load(path, Load.TURTLE_FORMAT);
        return g;
    }

    String read(String path) throws IOException {
        QueryLoad ql = QueryLoad.create();
        String res = ql.read(path);
        if (res == null) {
            throw new IOException(path);
        }
        return res;
    }

    String loadQuery(String path) throws IOException {
        if (isProtected && !path.startsWith(SERVER)) {
            throw new IOException(path);
        }
        return read(path);
    }

    Service getService(String name) {
        return map.get(name);
    }

    String getValues(String var, String resource) {
        if (var != null) {
            return "values " + var + " { <" + resource + "> }";
        }
        return null;
    }

    void init(Graph g) throws IOException, EngineException {
        String str = read(QUERY + "profile.rq");
        QueryProcess exec = QueryProcess.create(g);
        Mappings map = exec.query(str);
        for (Mapping m : map) {
            init(g, m);
        }
    }

    void init(Graph g, Mapping m) {
        Node prof   = m.getNode("?p");
        Node query  = m.getNode("?q");
        Node var    = m.getNode("?v");
        Node trans  = m.getNode("?t");
        Node serv   = m.getNode("?s");
        Node ctx    = m.getNode("?c");

        Service s = new Service(prof.getLabel());

        if (trans != null) {
            s.setTransform(trans.getLabel());
        }
        if (query != null) {
            s.setQuery(query.getLabel());
        }
        if (var != null) {
            s.setVariable(var.getLabel());
        }
        if (serv != null) {
            s.setServer(serv.getLabel());
        }      
        if (ctx != null){
            Context c = context(g, ctx);
            s.setParam(c);
        }
        map.put(prof.getLabel(), s);
    }
    
    /**
     * st:context [ st:param value ; st:title value ]
     * Create a Context, assign it to Service
     * Will be passed to transformation
     * @param ctx 
     */
    Context context(Graph g, Node ctx){
        Context c = context(new Context(), g, ctx);
        return c;
    }
        
    Context context(Context c, Graph g, Node ctx) {
        importer(c, g, ctx);

        for (Entity ent : g.getEdgeList(ctx)) {
            if (!ent.getEdge().getLabel().equals(Context.STL_IMPORT)) {
                Node object = ent.getNode(1);

                if (object.isBlank()) {
                    IDatatype list = list(g, object);
                    if (list != null) {
                        c.set(ent.getEdge().getLabel(), list);
                        continue;
                    }
                }
                c.set(ent.getEdge().getLabel(), (IDatatype) object.getValue());
            }
        }
        return c;
    }
    
     /** 
      *       
      * TODO: prevent loop 
      * n =  [ st:import st:cal ]
      * st:cal st:param [ ... ] 
      * 
      */
     void importer(Context c, Graph g, Node n){
         Edge imp = g.getEdge(Context.STL_IMPORT, n, 0);        
          if (imp != null){
                Edge par = g.getEdge(Context.STL_PARAM, imp.getNode(1), 0);
                if (par != null){
                    context(c, g, par.getNode(1));
                }
            }
     }
        
    
    IDatatype list(Graph g, Node object) {
        List<IDatatype> list = g.getDatatypeList(object);
        if (! list.isEmpty()) {           
            IDatatype dt = DatatypeMap.createList(list);
            return dt;
        }
        return null;
    }
    

    void initLoad(Graph g) throws IOException, EngineException {
        String str = read(QUERY + "profileLoad.rq");
        QueryProcess exec = QueryProcess.create(g);
        Mappings map = exec.query(str);
        for (Mapping m : map) {
            initLoad(m);
        }
    }

    void initLoad(Mapping m) {
        Node profile = m.getNode("?p");
        Node load = m.getNode("?ld");
        if (load == null) {
            return;
        }
        String[] list = load.getLabel().split(";");
        Service s = new Service(profile.getLabel());
        s.setLoad(list);
        map.put(profile.getLabel(), s);
    }

    void initLoader(Graph g) throws EngineException {
        String str = "select * where {"
                + "?s a st:Server "
                + "values ?p { st:data st:schema st:context }"
                + "?s ?p ?d "
                + "?d st:uri ?u "
                + "optional { ?d st:name ?n } "
                + "optional { ?s st:service ?sv } "
                + "}";
        QueryProcess exec = QueryProcess.create(g);
        Mappings map = exec.query(str);
        for (Mapping m : map) {
            Node sn = m.getNode("?s");
            Node sv = m.getNode("?sv");
            Node p = m.getNode("?p");
            Node u = m.getNode("?u");
            Node n = m.getNode("?n");
           Service s = findServer(sn.getLabel());
           s.setService((sv==null)?null:sv.getLabel());
           s.add(p.getLabel(), u.getLabel(), (n != null)?n.getLabel():null);
        }

    }

    Service findServer(String name) {
        Service s = getServer(name);
        if (s == null) {
            s = new Service(name);
            servers.put(name, s);
        }
        return s;
    }
    
    Service getServer(String name){
        return servers.get(name);
    }

}
