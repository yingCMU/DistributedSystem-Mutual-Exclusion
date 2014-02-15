package lab;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class Loggerwhisper {
	private String configuration_filename;
	private Configuration configuration;
	private String localName;
	private MessagePasser mp;
	private ClockService clockService;
	private String[] hostnameList;
	private Loggerclass logclass;
    private static HashMap<String,Clock> option = new HashMap<String,Clock>(); 

	public static void main(String[] args) {
		Loggerwhisper log = new Loggerwhisper();
		//Use the main thread to keep asking to the user if he wants to read the logs
				while(true){
					String answer;
					Scanner scanner = new Scanner (System.in);
					System.out.println("Do you want to read the logs(y/n)"); 
					answer = scanner.next();
					if(answer.equals("y"))
						log.logclass.displaylog();
					
				}
				
	}

	public Loggerwhisper(){
		Scanner scanner = new Scanner (System.in);
		System.out.println("Please enter the path to the configration file"); 
		configuration_filename = scanner.next();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(configuration_filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// ask user for local name
		System.out.println("Please enter the local name"); 
		localName = scanner.next();

		// parse YAML
		Constructor constructor = new Constructor(Configuration.class);
		TypeDescription carDescription = new TypeDescription(Configuration.class);
		carDescription.putListPropertyType("configuration", Configuration.Host.class);
		carDescription.putListPropertyType("sendRules", Configuration.Rule.class);
		carDescription.putListPropertyType("receiveRules", Configuration.Rule.class);
		constructor.addTypeDescription(carDescription);
		Yaml yamlParser = new Yaml(constructor);
		configuration = (Configuration) yamlParser.load(in);	
		hostnameList = new String[configuration.configuration.size()];
		ArrayList<String> tempArr = new ArrayList<String>();

		//Populate the hostnamelist used for clockfactory
		int i = 0;
		for (Configuration.Host host : configuration.configuration) {
			hostnameList[i] = host.name;
			i++;
			tempArr.add(host.name);
		}
		
		//Types of clock
		option.put("0", Clock.LOGICAL);
        option.put("1", Clock.VECTOR);
		System.out.println("Please enter Type of Clock"); 
		String clocktype = scanner.next();

		clockService = ClockFactory.getClockService(option.get(clocktype), localName, tempArr);
		try {
			mp = new MessagePasser(null,configuration_filename, localName, clockService);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		logclass = new Loggerclass();

		// start background receive thread
		(new Thread(new ReceiveThread(mp))).start();
		
		
	}

	// background thread to keep receiving and updating message
	private class ReceiveThread implements Runnable {
		private MessagePasser mp;
		// constructor
		public ReceiveThread(MessagePasser mp) {
			this.mp = mp;
		}

		public void run() {
			TimeStampedMessage msg = null;

			while (true) {
				try {
					msg = mp.receive();
				}
				catch (IllegalArgumentException e)
				{
					System.out.println("Clocks Do Not Match!");
					e.printStackTrace();
				}
				//If you get a message add it to the logs
				if (msg != null)
				{
					logclass.addlog(msg);
					System.out.println(msg.getTimeStamp().toString());
				}
				
			}
		}
	}
}
