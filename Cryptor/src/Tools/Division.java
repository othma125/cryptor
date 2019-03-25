package Tools;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Othmane
 */
public class Division {
    public StringNumber Quotient;
    public StringNumber Rest;
    public Division(StringNumber x,StringNumber y){
        if(y.GreaterTo(x)){
            this.Quotient=new StringNumber();
            this.Rest=x;
        }
        else if(y.EqualsTo(new StringNumber(1l))){
            this.Quotient=x;
            this.Rest=new StringNumber();
        }
        else{
            int taille=y.Value.length();
            String selection="";
            for(int j=0;j<taille;j++)
                selection+=((Character)x.Value.charAt(j)).toString();
            int i=taille;
            StringNumber a=new StringNumber(selection);
            if(y.GreaterTo(a)){
                i++;
                a.Value+=((Character)x.Value.charAt(taille)).toString();
            }
            StringNumber somme=new StringNumber();
            int n=0;
            while(a.GreaterOrEqualsTo(somme.Addition(y))){
                n++;
                somme=somme.Addition(y);
            }
            String q=String.valueOf(n),r=a.Soustraction(somme).Value;
            while(i<x.Value.length()){
                if(r.equals("0"))
                    selection=((Character)x.Value.charAt(i)).toString();
                else
                    selection=r+((Character)x.Value.charAt(i)).toString();
                a=new StringNumber(selection);
                somme=new StringNumber();
                n=0;
                while(a.GreaterOrEqualsTo(somme.Addition(y))){
                    n++;
                    somme=somme.Addition(y);
                }
                q+=String.valueOf(n);
                r=a.Soustraction(somme).Value;
                i++;
            }
            this.Quotient=new StringNumber(q);
            this.Rest=new StringNumber(r);
        }
    }
    public String toString(){
        return this.Quotient.toString()+" "+this.Rest.toString();
    }
}
