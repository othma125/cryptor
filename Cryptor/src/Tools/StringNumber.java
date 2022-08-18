package Tools;


import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.IntStream;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Othmane
 */
public class StringNumber {
    public String Value;
    
    public StringNumber(byte[] password){
        this.Value="0";
        StringNumber power=new StringNumber(1);
        int x;
        for(int i=0;i<password.length;i++){
            if(i>0)
                power=power.Multiplication(new StringNumber(256));
            x=password[i];
            if(x<0)
                x+=InputParameters._256;
            this.Value=new StringNumber(this).Addition(new StringNumber(x).Multiplication(power)).Value;
        }
    }
    
    public StringNumber(String valeur){
        this.Value=valeur;
    }
    
    public StringNumber(StringNumber NewStringNumber){
        this.Value=NewStringNumber.Value;
    }
    
    public StringNumber(){
        this.Value="0";
    }
    
    @Override
    public String toString(){
        return this.Value;
    }
    
    @Override
    public StringNumber clone(){
        return new StringNumber(this);
    }
    
//    void ToByteArray(Vector<Byte> v){
//        this.ToByteArray(v,InputParameters._256);
//    }
    
//    void ToByteArray(Vector<Byte> v,int base){
//        if(this.EqualsTo(new StringNumber()))
//            return;
//        Division d=new Division(this,new StringNumber(base));
//        v.addElement((byte)(int)Integer.valueOf(d.Rest.Value));
//        d.Quotient.ToByteArray(v,base);
//    }
    
    public static StringNumber getPassword(char[] password){
        StringNumber p=new StringNumber();
        StringNumber power=new StringNumber(1);
        for(int i=0;i<password.length;i++){
            if(i>0)
                power=power.Multiplication(new StringNumber(InputParameters._256));
            p=p.Addition(new StringNumber((int)password[i]).Multiplication(power));
        }
        return p;
    }
    
    private ArrayList<Character> toArrayList(){
        ArrayList<Character> list=new ArrayList<>();
        for(int i=0;i<this.Value.length();i++)
            list.add(this.Value.charAt(i));
        return list;
    }
    
    boolean EqualsTo(StringNumber x){
        if(this.Value.length()==x.Value.length()){
            ArrayList<Character> list1=this.toArrayList();
            ArrayList<Character> list2=x.toArrayList();
            return IntStream.range(0,this.Value.length())
                            .allMatch(i->Objects.equals(list1.get(i),list2.get(i)));
        }
        else
            return false;
    }
    
    boolean GreaterOrEqualsTo(StringNumber x){
        if(this.Value.length()==x.Value.length()){
            ArrayList<Character> list1=this.toArrayList();
            ArrayList<Character> list2=x.toArrayList();
            for(int i=0;i<this.Value.length();i++){
                if(Integer.valueOf(list1.get(i).toString())>Integer.valueOf(list2.get(i).toString()))
                    return true;
                else if(Integer.valueOf(list1.get(i).toString())<Integer.valueOf(list2.get(i).toString()))
                    return false;
            }
            return true;
        }
        else return this.Value.length()>x.Value.length();
    }
    
    boolean GreaterThan(StringNumber x){
        if(this.Value.length()==x.Value.length()){
            ArrayList<Character> list1=this.toArrayList();
            ArrayList<Character> list2=x.toArrayList();
            for(int i=0;i<this.Value.length();i++){
                if(Integer.valueOf(list1.get(i).toString())>Integer.valueOf(list2.get(i).toString()))
                    return true;
                else if(Integer.valueOf(list1.get(i).toString())<Integer.valueOf(list2.get(i).toString()))
                    return false;
            }
            return false;
        }
        else
            return this.Value.length()>x.Value.length();
    }
    
    StringNumber Power(int n){
        if(n==0)
            return new StringNumber(1);
        else if(n==1)
            return this;
        else{
            StringNumber x=new StringNumber(1);
            for(int i=0;i<n;i++)
                x=this.Multiplication(x);
            return x;
        }
    }
    
    StringNumber Soustraction(StringNumber x){
        String resultat="";
        int stored_value=0;
        ArrayList<Character> list1=this.toArrayList();
        ArrayList<Character> list2=x.toArrayList();
        do{
            int k1=0,k2=0;
            if(!list1.isEmpty()){
                k1=Integer.valueOf(list1.get(list1.size()-1).toString());
                list1.remove(list1.size()-1);
            }
            if(!list2.isEmpty()){
                k2=Integer.valueOf(list2.get(list2.size()-1).toString());
                list2.remove(list2.size()-1);
            }
            int somme=k1-k2-stored_value;
            stored_value=0;
            if(somme<0){
                somme+=10;
                stored_value++;
            }
            if(!list1.isEmpty() || (list1.isEmpty() && somme!=0))
                resultat=String.valueOf(somme)+resultat;
        }while(!list1.isEmpty());
        String r="";
        boolean condition=false;
        for(int i=0;i<resultat.length();i++){
            if('0'!=resultat.charAt(i)){
                condition=true;
                r=r+resultat.charAt(i);
            }
            else if(condition)
                r=r+resultat.charAt(i);
        }
        if(r.equals(""))
            return new StringNumber();
        return new StringNumber(r);
    }
    public StringNumber Addition(StringNumber x){
        String resultat="";
        String ajout=null;
        ArrayList<Character> list1=this.toArrayList();
        ArrayList<Character> list2=x.toArrayList();
        do{
            int k1=0,k2=0,k3=0;
            if(!list1.isEmpty()){
                k1=Integer.valueOf(list1.get(list1.size()-1).toString());
                list1.remove(list1.size()-1);
            }
            if(!list2.isEmpty()){
                k2=Integer.valueOf(list2.get(list2.size()-1).toString());
                list2.remove(list2.size()-1);
            }
            if(ajout!=null)
                k3=Integer.valueOf(ajout);
            int somme=k1+k2+k3;
            if(somme>9){
                ajout=String.valueOf(somme/10);
                resultat=String.valueOf(somme%10)+resultat;
            }
            else{
                ajout=null;
                resultat=String.valueOf(somme%10)+resultat;
            }
        }while(!list1.isEmpty() || !list2.isEmpty() || ajout!=null);
        return new StringNumber(resultat);
    }
    
    static StringNumber Factorial(int n){
        return (n==0)?new StringNumber(1):new StringNumber(n).Multiplication(Factorial(n-1));
    }
    
    StringNumber Multiplication(StringNumber x){
        if(this.Value.length()>x.Value.length())
            return x.Multiplication(this);
        StringNumber zero=new StringNumber();
        if(x.EqualsTo(zero) || x.EqualsTo(zero))
            return zero;
        else if(x.EqualsTo(new StringNumber(1)))
            return this;
        else if(this.EqualsTo(new StringNumber(1)))
            return x;
        else{
            int n=0;
            StringNumber resultat=new StringNumber();
            for(int i=this.Value.length()-1;i>-1;i--){
                StringNumber y=new StringNumber();
                Character c=this.Value.charAt(i);
                int k=Integer.valueOf(c.toString());
                for(int j=0;j<k;j++)
                    y=y.Addition(x);
                for(int j=0;j<n;j++)
                    y.Value+="0";
                resultat=resultat.Addition(y);
                n++;
            }
            return resultat;
        }
    }
    
    public StringNumber(long l){
        this.Value=Long.toString(l);
    }
    
    public StringNumber(int x){
        this.Value=Integer.toString(x);
    }
    
    long toInteger(){
        return Long.valueOf(this.Value);
    }
}
