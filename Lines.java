import java.util.*;
import java.io.*;

public class Lines {

  Map<String, Double> wires;
  Map<String, Double> prices;
  ArrayList<String> wire_set;
  Scanner scan;
  Scanner sc;

  public Lines() throws FileNotFoundException {
    scan = new Scanner(new File("catalog4.txt"));
    sc = new Scanner(new File("catalog3.txt"));
    create_wires();
    price_it();
  }



  //Creates map for individual wire and their sizes
  public void create_wires() {
    wires = new TreeMap<String,Double>();
    wire_set = new ArrayList<String>();
    wires = new LinkedHashMap<>();
    scan.nextLine();
    String line = scan.nextLine();
    line = line.substring(1,line.length()-1);
    String[]pairs = line.split(", ");
    for(int i = 0; i < pairs.length; i++) {
      String[]val = pairs[i].split("=");
      String type = val[0];
      Double area = Double.parseDouble(val[1]);
      wires.put(type,area);
      wire_set.add(type);
    }
  }

  public void price_it() {
    prices = new TreeMap<String,Double>();
    prices = new LinkedHashMap<>();
    sc.nextLine();
    String line = sc.nextLine();
    while(sc.hasNextLine()){
      //System.err.println(line);
      String[]val = line.split("=");
      String type = val[0];
      Double pr = Double.parseDouble(val[1]);
      prices.put(type,pr);
      line = sc.nextLine();
    }
  }

  public double res(String wr){
    return wires.get(wr);
  }

  public double pr(String wr){
    return prices.get(wr);
  }
  //Determines if given wire is listed in map
  public boolean contain_wire(String s){
    return wires.containsKey(s);
  }

  public ArrayList<String> get_wires(){
    return wire_set;
  }

  public int get_index(String word){
    return wire_set.indexOf(word);
  }



}
