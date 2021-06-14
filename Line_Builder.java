import java.io.*;
import javax.imageio.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.*;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;



/*
  The CFLL or Conduit Fill and Line Loss calculator was developed to help in calculations for Traffic Engineering projects.
  The Conduit Fill section allows the user to calculate the percentage filled for a new or exisitng Conduit and if the conduit is new it
  will give the minimum sized conduit allowed for the given area, this is purely for user to know the most cost effective size they can have.
  The Line Loss sections allows the user to calculate percentage loss for indvidual branches.
  This tool also prints out all the information calculated into a text file that can be used in future reports or designs and makes all the
  information easily accessible.
*/

public class Line_Builder{
  public static File file;
  public static File file2;
  public static double min_value;
  //Main Function sets the initial UI look and starts the program
  public static void main(String[] args) throws FileNotFoundException {
    setNimbus();
    PrintStream output;
    SwingUtilities.invokeLater(() -> {
      Intro();
    });
  }
    //Prompts the user to create their new file
    public static void Intro() {
      JFrame frame = set_small_frame(800,115);
      JButton SUB = new JButton("Create your file");
      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(SUB, BorderLayout.NORTH);
      frame.add(mainPanel, BorderLayout.CENTER);
      SUB.addActionListener((e) -> {
        get_file();
        String name = file.getName();
        name = name.substring(0,name.length()-4);
        try
        {
          PrintStream  output = new PrintStream(file);
          Choose(name,output);
          frame.setVisible(false);
        }
        catch(FileNotFoundException b)
        {
          b.printStackTrace();
        }
      });
    }

    //Prompts the user to create their new file
    public static void Choose(String name, PrintStream output) {
      JFrame frame = set_small_frame(800,115);
      JButton SUB = new JButton("Choose your wiring file");
      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(SUB, BorderLayout.NORTH);
      frame.add(mainPanel, BorderLayout.CENTER);
      SUB.addActionListener((e) -> {
        get_file2();

        try
        {
          Scanner scan = new Scanner(file2);
          frame.setVisible(false);
          Start(name,output,scan);

        }
        catch(FileNotFoundException b)
        {
          b.printStackTrace();
        }
      });
    }

    //Gives start button
    public static void Start(String name, PrintStream output, Scanner scan) throws FileNotFoundException{
      JFrame frame = set_small_frame(800,115);
      JButton SUB = new JButton("Start Calculating");
      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(SUB, BorderLayout.NORTH);
      frame.add(mainPanel, BorderLayout.CENTER);
      SUB.addActionListener((e) -> {

        try
        {
          create(name,output,scan);
          frame.setVisible(false);
        }
        catch(FileNotFoundException b)
        {
          b.printStackTrace();
        }
      });
    }

    //Builds map that holds segment #, segment length, and segment amperes
    public static void create(String name, PrintStream output, Scanner scan) throws FileNotFoundException{
      String date = get_date();
      output.println("                                                            " + date);
      output.println("                            " + name + " Design");
      output.println();
      int cases = scan.nextInt();
      for(int i = 0; i < cases; i++){
        String branch = scan.next();
        int volt = scan.nextInt();
        int seg = scan.nextInt();
        String start = scan.next();
        String end = scan.next();
        int length = 0;
        Map<Integer,len_wire> segs = new TreeMap<Integer,len_wire>();
        for(int j = 1; j < seg + 1; j++){
          double amp = scan.nextDouble();
          int len = scan.nextInt();
          len_wire temp = new len_wire(amp,len);
          length += len;
          segs.put(j,temp);
        }
        output.println("Branch: " + branch);
        calc(segs,volt,seg,length,output,start,end);
        output.println();
      }
      openFile();
    }

    //Recursion to build the wire run that is most cost efficient
    public static void calc(Map<Integer,len_wire> segs,int volt,int size, int length, PrintStream output,String start, String end) throws FileNotFoundException   {
      double five_per = .05 * volt;
      Lines line = new Lines();
      min_value = length * line.pr(end) * 2;
      double min_price = length * line.pr(start) * 2;
      double starting_min = min_value;
      min_price = Math.round(min_price*100.00)/100.00;
      ArrayList<String> sets = line.get_wires();
      Map<Integer,String> wires = new TreeMap<Integer,String>();
      Map<Integer,String> fin = new TreeMap<Integer,String>();
      check_sums temp = new check_sums();
      check_sums finals = new check_sums();
      int first = line.get_index(start);
      int last = line.get_index(end);
      temp.check(first,last);
      finals = build_seg(temp.get_price(),temp.get_start(),temp.get_end(),temp.get_seg(),temp.get_load(),temp.get_Map(),temp.validation(),sets,segs,size,line,five_per);
      output.println("Tot = " + volt + " V, 5% = " + five_per + " V, Total Length = " + length + " ft");
      output.println("Least Expensive = $" + min_price + " Most expensive = $" + starting_min);
      output.println("Smallest wire desired = " + start + ", Largest wire desired = " + end);
      if(finals.get_Map().entrySet().isEmpty()){
        output.println("No solution found for this range.");
      } else {
        double price = Math.round(finals.get_price()*100.00)/100.00;
        double load = Math.round(finals.get_load()*100.00)/100.00;
        output.println("Most Cost Efficient Price = $" + price);
        output.println("Final Load = " + load + " V");
        output.println("Seg    Len    AMP    WIRE");
        double check_price = 0.0;
        for(int i = 1; i < size + 1;i++){
          print_value(output,i);
          print_value(output,segs.get(i).get_Len());
          print_value(output,segs.get(i).get_Amp());
          print_value(output,finals.get_Map().get(i));
          output.println();
        }
      }
    }

    //Recursion that builds runs by cost efficiency and load efficiency
    public static check_sums build_seg(double price, int start, int fin, int seg, double load,Map<Integer, String> build, boolean validation, ArrayList<String> sets, Map<Integer, len_wire> segs, int size,Lines line, double five_per){
      Map<Integer,String> help = copyMe(build);
      if(seg == size) {
        check_sums end = new check_sums(price,start,seg,load,help,validation);
        double temp_value = end.get_price();
        if((temp_value < min_value) && load < five_per){
          min_value = Math.min(min_value,temp_value);
        } else {
          end.invalid();
        }
        return end;
      } else if (load > five_per )  {
          check_sums end = new check_sums(price,start,seg,load,help,validation);
          end.invalid();
          return end;
      } else if(price > min_value){
        check_sums end = new check_sums(price,start,seg,load,help,validation);
        end.invalid();
        return end;
      } else {
        check_sums end = new check_sums();
        ArrayList<check_sums> list = new ArrayList<check_sums>();
        end.invalid();
        for(int i = start; i < fin+1; i++){
          check_sums temp = new check_sums(price,start,seg,load,help,validation);
          temp.next();
          check_sums temp2 = new check_sums();
          String wire = sets.get(i);
          int len = segs.get(temp.seg).get_Len();
          double amp = segs.get(temp.seg).get_Amp();
          double res = line.res(wire);
          double cost = line.pr(wire) * len * 2;
          double ld = len * res * amp * 2;
          temp.add(ld,cost,wire);
          temp.check(i,fin);
          temp2 = build_seg(temp.get_price(),temp.get_start(),temp.get_end(),temp.get_seg(),temp.get_load(),temp.get_Map(),temp.validation(),sets,segs,size,line,five_per);
          if(temp2.validation()){
            end = new check_sums(temp2.get_price(),temp2.get_start(),temp2.get_seg(),temp2.get_load(),temp2.get_Map(),temp2.validation());
          }
        }
        return end;
      }
    }

    //Copys maps
    public static Map<Integer, String> copyMe(Map<Integer,String> orig){
      Map<Integer,String> copy = new TreeMap<Integer,String>();
      Set<Integer> keys = orig.keySet();
      Iterator<Integer> look = keys.iterator();
      while(look.hasNext()){
        int key = look.next();
        copy.put(key,orig.get(key));
      }
      return copy;
    }

    //Sets the frame size and information
    public static JFrame set_small_frame(int x, int y) {
      JFrame frame = new JFrame();
      BufferedImage img = null;
      try {
        img = ImageIO.read(new File("LOGO ONLY PNG.png"));
      } catch (IOException e) {
      }
      frame.setTitle("Line Designer (Proprietary PH Consulting use only)");
      frame.setLayout(new BorderLayout());
      JLabel rules = new JLabel(" Unlicensed use prohibited, contact admin@phtraffic.com for licensing");
      frame.add(rules,BorderLayout.PAGE_END);
      frame.setSize(new Dimension(x, y));
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setLocationRelativeTo(null);
      frame.setResizable(false);
      frame.setIconImage(img);
      frame.setVisible(true);
      return frame;
    }

    //Gets current date
    public static String get_date() {
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
      LocalDateTime now = LocalDateTime.now();
      return dtf.format(now);
    }

    //Allows user to pick a save spot for their file
    public static void get_file(){
      setWindows();
      JFileChooser chooser = new JFileChooser();
  		chooser.showSaveDialog(null);
  		file = chooser.getSelectedFile();
      file = new File(file.getPath()+".txt");
      setNimbus();
    }

    //Allows user to pick a save spot for their file
    public static void get_file2(){
      setWindows();
      JFileChooser chooser = new JFileChooser();
  		chooser.showOpenDialog(null);
  		file2 = chooser.getSelectedFile();
      System.err.println(file2.getPath());
      setNimbus();
    }

    //Sets look of UI
    public static void setNimbus() {
      try {
          UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
      } catch (Exception e) {
          e.printStackTrace();
      }
    }

    //Sets look of UI
    public static void setWindows() {
      try {
          UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
      } catch (Exception e) {
          e.printStackTrace();
      }
    }

    //Opens the new file when finished
    public static void openFile() {
      try
      {
        if(!Desktop.isDesktopSupported())
        {
          System.out.println("not supported");
          return;
        }
        Desktop desktop = Desktop.getDesktop();
        if(file.exists())
        desktop.open(file);
        System.exit(0);
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }

    //To print table
    private static void print_value(PrintStream output, String wd) {
      output.print(wd);
      for(int i = 0; i < 7 - wd.length(); i++) {
        output.print(" ");
      }
    }

    //To print table
    private static void print_value(PrintStream output, int wd) {
      output.print(wd);
      String w = String.valueOf(wd);
      for(int i = 0; i < 7 - w.length(); i++) {
        output.print(" ");
      }
    }

    //To print table
    private static void print_value(PrintStream output, double wd) {
      output.print(wd);
      String w = String.valueOf(wd);
      for(int i = 0; i < 7 - w.length(); i++) {
        output.print(" ");
      }
    }
}

//Object that holds initial lenght and amplitude of segments
class len_wire{
  double amp;
  int len;

  public len_wire(double ampere, int length){
    amp = ampere;
    len = length;
  }

  public double get_Amp(){
    return amp;
  }

  public int get_Len(){
    return len;
  }
}

//Object that holds the branches, segments, price, load.
class check_sums{
  double price;
  double load;
  int start;
  int end;
  int seg;
  boolean valid;
  Map<Integer,String> build;
  public check_sums(){
    this.price = 0.0;
    this.start = 0;
    this.end = 0;
    this.seg = 0;
    this.load = 0.0;
    this.build = new TreeMap<Integer,String>();

    this.valid = true;
  }

  public check_sums(double pr, int st, int sg, double ld,Map<Integer, String> bd, boolean vd){
    this.price = pr;
    this.load = ld;
    this.start = st;
    this.seg = sg;
    this.valid = vd;
    this.build = bd;
  }

  public void add(double ld, double pr,String wire){
    this.load+=ld;
    this.price+=pr;
    this.build.put(seg,wire);
    //System.out.println(price);
  }
  public void next(){
    seg++;
  }

  public void back(){
    seg--;
  }

  public void check(int type, int fin){
    this.start = type;
    this.end = fin;
  }
  public void invalid(){
    this.valid = false;
  }

  public double get_price(){
    return price;
  }

  public boolean validation(){
    return valid;
  }

  public int get_start(){
    return start;
  }

  public int get_end(){
    return end;
  }

  public int get_seg(){
    return seg;
  }

  public double get_load(){
    return load;
  }

  public Map<Integer,String> get_Map(){
    return build;
  }
}
