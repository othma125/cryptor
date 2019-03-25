package Tools;


import java.util.Objects;
import java.util.Vector;

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
//    public void ToByteArray(Vector<Byte> v){
//        this.ToByteArray(v,InputParameters._256);
//    }
//    public void ToByteArray(Vector<Byte> v,int base){
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
    private Vector<Character> toVector(){
        Vector<Character> v=new Vector<>();
        for(int i=0;i<this.Value.length();i++)
            v.addElement(this.Value.charAt(i));
        return v;
    }
    public boolean EqualsTo(StringNumber x){
        if(this.Value.length()==x.Value.length()){
            Vector<Character> v1=this.toVector();
            Vector<Character> v2=x.toVector();
            for(int i=0;i<this.Value.length();i++){
                if(!Objects.equals(v1.elementAt(i),v2.elementAt(i)))
                    return false;
            }
            return true;
        }
        else return false;
    }
    public boolean GreaterOrEqualsTo(StringNumber x){
        if(this.Value.length()==x.Value.length()){
            Vector<Character> v1=this.toVector();
            Vector<Character> v2=x.toVector();
            for(int i=0;i<this.Value.length();i++){
                if(Integer.valueOf(v1.elementAt(i).toString())>Integer.valueOf(v2.elementAt(i).toString()))
                    return true;
                else if(Integer.valueOf(v1.elementAt(i).toString())<Integer.valueOf(v2.elementAt(i).toString()))
                    return false;
            }
            return true;
        }
        else return this.Value.length()>x.Value.length();
    }
    public boolean GreaterTo(StringNumber x){
        if(this.Value.length()==x.Value.length()){
            Vector<Character> v1=this.toVector();
            Vector<Character> v2=x.toVector();
            for(int i=0;i<this.Value.length();i++){
                if(Integer.valueOf(v1.elementAt(i).toString())>Integer.valueOf(v2.elementAt(i).toString()))
                    return true;
                else if(Integer.valueOf(v1.elementAt(i).toString())<Integer.valueOf(v2.elementAt(i).toString()))
                    return false;
            }
            return false;
        }
        else return this.Value.length()>x.Value.length();
    }
    public StringNumber Power(int n){
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
    public StringNumber Soustraction(StringNumber x){
        String resultat="";
        int ajout=0;
        Vector<Character> v1=this.toVector();
        Vector<Character> v2=x.toVector();
        do{
            int k1=0,k2=0;
            if(!v1.isEmpty()){
                k1=Integer.valueOf(v1.lastElement().toString());
                v1.remove(v1.size()-1);
            }
            if(!v2.isEmpty()){
                k2=Integer.valueOf(v2.lastElement().toString());
                v2.remove(v2.size()-1);
            }
            int somme=k1-k2-ajout;
            ajout=0;
            if(somme<0){
                somme+=10;
                ajout++;
            }
            if(!v1.isEmpty() || (v1.isEmpty() && somme!=0))
                resultat=String.valueOf(somme)+resultat;
        }while(!v1.isEmpty());
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
        Vector<Character> v1=this.toVector();
        Vector<Character> v2=x.toVector();
        do{
            int k1=0,k2=0,k3=0;
            if(!v1.isEmpty()){
                k1=Integer.valueOf(v1.lastElement().toString());
                v1.remove(v1.size()-1);
            }
            if(!v2.isEmpty()){
                k2=Integer.valueOf(v2.lastElement().toString());
                v2.remove(v2.size()-1);
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
        }while(!v1.isEmpty() || !v2.isEmpty() || ajout!=null);
        return new StringNumber(resultat);
    }
    public static StringNumber Factorial(int n){
        if(n==0)
            return new StringNumber(1);
        else
            return new StringNumber(n).Multiplication(Factorial(n-1));
    }
    public StringNumber Multiplication(StringNumber x){
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
    public long toInteger(){
        return Long.valueOf(this.Value);
    }
}
