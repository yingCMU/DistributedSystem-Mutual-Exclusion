package lab;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/*
 * Configuration
 * Author: Ying Li (yingl2)
 *         Utsav Drolia (udrolia)
 * Date: 2/15/2014
 */

public class Configuration {
    public List<Host> configuration = null;
    public List<Rule> sendRules = null;
    public List<Rule> receiveRules = null;
    public List<Group> groups = null;
    public List<Group> myGroups = new ArrayList<Group>();
    public Map<String, Host> hostMap = new HashMap<String, Host>();
    public Map<String, List<String>> groupMap = new HashMap<String, List<String>>();
   
    // a helpful but ugly function to initialize a map for host list ***
    public void initialize() {
        for (Host host : configuration) {
            hostMap.put(host.name, host);
            System.out.println("host: "+host.name+" memberOf: "+host.memberOf);
        }
        for (Group group : groups)
        {
        		System.out.println("Group:"+ group.name + " Members:" + group.members.toString());
        	
        	groupMap.put(group.name, group.members);
        }
        
        
    }
    public List<Group> getMyGroups(String local_name){
    	for (Group group : groups)
        {
        	if(group.members.contains(local_name)){
        		System.out.println("Group:"+ group.name + " Members:" + group.members.toString());
        		myGroups.add(group);
        	}
        	groupMap.put(group.name, group.members);
        }
    	return myGroups;
    }
    public static class Group {
    	public String name = null;
    	public List<String> members = null;
    }
    
    public static class Host {
        public String name = null;
        public String ip = null;
        public int port = -1;    // long?
        public List<String> memberOf=new ArrayList<String>();
    }
    
    public static class Rule {
        public String action = null;
        public String src = null;
        public String dest = null;
        public String kind = null;
        public int seqNum = -1;
        public Boolean dupe = null;
        
        public boolean match(Message msg) {
            if ((src != null && !src.equals(msg.getSource())) || 
                (dest != null && !dest.equals(msg.getDest())) || 
                (kind != null && !kind.equals(msg.getKind())) || 
                (seqNum != -1 && seqNum != msg.getSeqNum()) || 
                (dupe != null && !dupe.equals(msg.getDupe()))) {
                return false;
            } else {
                return true;
            }
        }
    }
    
    public static enum Action {
        DELAY ("delay"),
        DROP("drop"),
        DUPLICATE("duplicate");

        public String action;

        Action(String action){
            this.action = action;
        }
        
        public String getAction() {
            return this.action;
        }
    }
    /*
    private static void configurationParse(String filename) {
        try {
            // read file
            BufferedReader in = getFileByPath(filename);
            
            // parse YAML
            Constructor constructor = new Constructor(Configuration.class);
            TypeDescription carDescription = new TypeDescription(Configuration.class);
            carDescription.putListPropertyType("configuration", Configuration.Host.class);
            carDescription.putListPropertyType("sendRules", Configuration.Rule.class);
            carDescription.putListPropertyType("receiveRules", Configuration.Rule.class);
            carDescription.putListPropertyType("groups", Configuration.Group.class);
            constructor.addTypeDescription(carDescription);
            Yaml yamlParser = new Yaml(constructor);
            
            Configuration conf = (Configuration) yamlParser.load(in);
            conf.initialize();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static BufferedReader getFileByPath(String configuration_filename) throws FileNotFoundException {
        return new BufferedReader(new FileReader(configuration_filename));
    }
     public static void main(String[] args){
    	configurationParse("./conf4.txt");
    	
    }*/
    
}
