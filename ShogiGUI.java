import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.function.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class ShogiGUI extends JFrame implements MouseListener{
	private static final String ENCODE=System.getProperty("ShogiGUI.input_encoding","UTF-8");
	private static final String MSG_SELECT="Selected %s (Click here to cancel)";
	private static final String MSG_EXEC="Moving %s from %d,%d to %d,%d";
	private static final String MSG_ERR_ENEMY="Error: %s is enemy's piece";
	private static final String MSG_ERR_CANNOT_MOVE="Error: %s can't move to %d,%d";
	private static final String MSG_ERR_INTERNAL_ERROR="Error: Internal error. Program will shutdown in 10sec.";
	private static final String MSG_ERR_99SHOGI_EXITED="Error: 99shogi exited. Program will shutdown in 10sec.";
	private static final String MSG_ERR_99SHOGI_DOES_NOT_RESPONSE="Error: 99shogi does not response by 10sec.";
	private static final String MSG_NARU="Do you want to naru %s to %s?";
	private static Process proc;
	private static Scanner pin;
	private static PrintStream pout;
	private static ShogiGUI main;
	private JTable table;
	private JList ownpiece;
	private JList eownpiece;
	//private JPanel panel;
	private JButton message;
	private int row,col=-1;
	public ShogiGUI(String title){
		setTitle(title);
		setBounds(10,10,250,230);
		Object data[][]=new Object[9][10];
		data[0][9]="一";
		data[1][9]="二";
		data[2][9]="三";
		data[3][9]="四";
		data[4][9]="五";
		data[5][9]="六";
		data[6][9]="七";
		data[7][9]="八";
		data[8][9]="九";
		table=new JTable(data,new Object[]{9,8,7,6,5,4,3,2,1,"／"});
		table.addMouseListener(this);
		table.setDefaultEditor(Object.class,null);
		table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		table.getTableHeader().setResizingAllowed(false);
		table.getTableHeader().setReorderingAllowed(false);
		message=new JButton();
		message.setText("Shogi GUI");
		message.addActionListener(e->{
			col=-1;
			table.clearSelection();
			message.setText("Shogi GUI");
		});
		//panel=new JPanel();
		//panel.setLayout(new BorderLayout());
		getContentPane().add(new JScrollPane(table,ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),BorderLayout.CENTER);
		getContentPane().add(message,BorderLayout.SOUTH);
		//getContentPane().add(panel,BorderLayout.CENTER);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	public static void main(String[]args)throws IOException{
		if(args.length==1&&args[0].equals("-h"))help();
		ProcessBuilder pb=new ProcessBuilder(args.length==0?new String[]{"a.out"}:args);
		pb.redirectErrorStream(true);
		proc=pb.start();
		pin=new Scanner(proc.getInputStream(),ENCODE);
		pout=new PrintStream(proc.getOutputStream(),true);
		main=new ShogiGUI("Shogi GUI");
		new Thread(main::cui).start();
		new Thread(main::in).start();
		main.setVisible(true);
	}
	public String read(){
		try{
			String s=pin.nextLine();
			System.out.println(s);
			return s;
		}catch(NoSuchElementException e){
			throw e;
		}catch(Exception e){
			e(e);
			return null;
		}
	}
	public void cui(){
		try{
			String line;
			//Thread t=Thread.currentThread();
			while(true){
				line=read().trim();
				if(line.equals("9  8  7  6  5  4  3  2  1")){
					try{
						SwingUtilities.invokeAndWait(()->{
							for(int i=0;i<9;++i){
								String l=read().replace("* ","*");
								if(l.isEmpty()){
									--i;continue;
								}
								table.setValueAt(l.substring(0,2),i,0);
								table.setValueAt(l.substring(2,4),i,1);
								table.setValueAt(l.substring(4,6),i,2);
								table.setValueAt(l.substring(6,8),i,3);
								table.setValueAt(l.substring(8,10),i,4);
								table.setValueAt(l.substring(10,12),i,5);
								table.setValueAt(l.substring(12,14),i,6);
								table.setValueAt(l.substring(14,16),i,7);
								table.setValueAt(l.substring(16,18),i,8);
							}
							message.setText("Shogi GUI");
						});
					}catch(NoSuchElementException e){
						throw e;
					}catch(Exception e){e(e);}
				}else if(line.equals("*** YOU WIN! ***")){
					message.setText("*** YOU WIN! ***");
					while(true){
						try{
							for(ActionListener a:message.getActionListeners()){
								message.removeActionListener(a);
							}
							break;
						}catch(Throwable t){}
					}
					synchronized(MSG_EXEC){}
				}else if(line.equals("*** YOU LOSE ***")){
					message.setText("*** YOU LOSE ***");
					while(true){
						try{
							for(ActionListener a:message.getActionListeners()){
								message.removeActionListener(a);
							}
							break;
						}catch(Throwable t){}
					}
					synchronized(MSG_EXEC){}
				}
			}
		}catch(NoSuchElementException e){
			message.setText(MSG_ERR_99SHOGI_EXITED);
		}catch(Exception e){
			message.setText(MSG_ERR_INTERNAL_ERROR);
		}finally{
			while(true){
				try{
					Thread.sleep(10000);
					System.exit(0);
				}catch(Throwable t){}
			}
		}
	}
	public String getName(String n){
		switch(n){
			case"+歩":return"FU";
			case"+香":return"KY";
			case"+桂":return"KE";
			case"+銀":return"GI";
			case"+金":return"KI";
			case"+角":return"KA";
			case"+飛":return"HI";
			case"+と":return"TO";
			case"+杏":return"NY";
			case"+圭":return"NK";
			case"+全":return"NG";
			case"+馬":return"UM";
			case"+竜":return"RY";
			case"+玉":return"GY";
			case" *":return "";
			default:return null;
		}
	}
	public String checkName(String s,int from_i,int to_i){
		String name=getName(s);
		if(name==null){
			message.setText(String.format(MSG_ERR_ENEMY,s));
		}else if(name.isEmpty()){
			message.setText("Shogi GUI");
			table.clearSelection();
			col=-1;
			throw new IllegalArgumentException();
		}else if(from_i<3||to_i<3){
			String k=null;
			switch(name){
				case"FU":return"TO";
				case"KI":return"KI";
				case"KA":return"UM";
				case"HI":return"RY";
				case"TO":return"TO";
				case"NY":return"NY";
				case"NK":return"NK";
				case"NG":return"NG";
				case"UM":return"UM";
				case"RY":return"RY";
				case"GY":return"GY";
				case"KE":
					if(to_i==1)return"KE";
					k="NK";
				case"KY":
					if(k==null)k="NY";
					if(to_i==0)return k;
				case"GI":
					if(k==null)k="NG";
					switch(JOptionPane.showConfirmDialog(this,String.format(MSG_NARU,name,k))){
						case JOptionPane.YES_OPTION:
							return k;
						case JOptionPane.NO_OPTION:
							return name;
						case JOptionPane.CANCEL_OPTION:
							throw new IllegalArgumentException();
						default:
							throw new IllegalStateException();
					}
				default:
					throw new IllegalStateException();
			}
		}else return name;
		throw new IllegalArgumentException();
	}
	public int[]checkInterval(int[]c){
		if(c.length==0)return new int[]{-1,-1};
		if(c.length==1)return new int[]{c[0],c[0]};
		if(c[0]<c[1]){
			int i=c[0]-1;
			for(int e:c)if(++i!=e)throw new IllegalArgumentException();
			return new int[]{c[0],c[c.length-1]};
		}else{
			int i=c[0]+1;
			for(int e:c)if(--i!=e)throw new IllegalArgumentException();
			return new int[]{c[c.length-1],c[0]};
		}
	}
	public void checkMove(int col,int row,int tocol,int torow){
		message.setText(String.format(MSG_EXEC,table.getValueAt(row,col).toString().charAt(1),9-col,row+1,9-tocol,torow+1));
		// TODO: Add check program.
	}
	public void mouseClicked(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
	public void mouseExited(MouseEvent e){}
	public void mousePressed(MouseEvent e){
		try{
			int[]ci=checkInterval(table.getSelectedColumns());
			int[]ri=checkInterval(table.getSelectedRows());
			if(ci[0]!=ci[1]||ri[0]!=ri[1])return;
			if(col==-1){
				if(table.getValueAt(ri[0],ci[0]).equals(" *")){
					table.clearSelection();
					message.setText("Shogi GUI");
				}else{
					message.setText(String.format(MSG_SELECT,(9-ci[0])+","+(ri[0]+1)));
					col=ci[0];row=ri[0];
				}
			}else if(ci[0]!=col||ri[0]!=row){
				table.setColumnSelectionInterval(col,ci[0]);
				table.setRowSelectionInterval(row,ri[0]);
			}
		}catch(IllegalArgumentException ex){
			//ignore
		}catch(Exception ex){e(ex);}
	}
	public void mouseReleased(MouseEvent e){
		try{
			int[]ci=checkInterval(table.getSelectedColumns());
			int[]ri=checkInterval(table.getSelectedRows());
			if(ci[1]==9)table.setColumnSelectionInterval(ci[0]==9?ci[0]=8:ci[0],ci[1]=8);
			int tocol,torow;
			if(col==-1)return;
			if(ci[0]==ci[1]&&ri[0]==ri[1]){
				if(ci[0]==col&&ri[0]==row)return;
				ci[0]=col;ri[0]=row;
			}
			if(ci[0]==col){
				if(ri[0]==row){
					tocol=ci[1];
					torow=ri[1];
				}else if(ri[1]==row){
					tocol=ci[1];
					torow=ri[0];
				}else return;
			}else if(ci[1]==col){
				if(ri[0]==row){
					tocol=ci[0];
					torow=ri[1];
				}else if(ri[1]==row){
					tocol=ci[0];
					torow=ri[0];
				}else return;
			}else return;
			checkMove(col,row,tocol,torow);
			String out_string=String.format("%d%d%d%d%s",9-col,row+1,9-tocol,torow+1,checkName(table.getValueAt(row,col).toString(),row,torow));
			pout.println(out_string);
			System.out.println(out_string);
			col=-1;
		}catch(IllegalArgumentException ex){
			//ignore
		}catch(Exception ex){e(ex);}
	}
	public void in(){
		while(true){
			try{
				pout.write(System.in.read());
			}catch(IOException e){
				e(e);
			}
		}
	}
	public static<T>void forEach(Enumeration<T>e,Consumer<T>c){
		while(e.hasMoreElements()){
			c.accept(e.nextElement());
		}
	}
	public static void e(Exception e){
		System.err.printf("ShogiGUI:Exception in thread \"%s\" ",Thread.currentThread().getName());
		e.printStackTrace();
	}
	public static void help(){
		System.out.println("Usage:");
		System.out.println("java ShogiGUI -h");
		System.out.println("java ShogiGUI [executable file]");
		System.out.println();
		System.out.println();
		System.out.println("               -h Show this help.");
		System.out.println();
		System.out.println("[executable file] Executable file of 99shogi.");
		System.out.println("                  Default: a.out");
		System.exit(0);
	}
}
