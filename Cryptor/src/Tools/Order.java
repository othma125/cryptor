package Tools;

import java.util.stream.IntStream;
import java.util.stream.Stream;



/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Othmane
 */
public class Order{
    private static final short[] DefaultOrder={0,4,5,12,1,2,9,13,8,6,15,14,3,7,11,10};
    private static final byte[][] SecretMatrix={null,
                                    {1,4},
                                    {2,3,5,8,12},
                                    {3,5,8,12},
                                    null,
                                    {5,8,12},
                                    {6,7,9,13},
                                    {7,9,13},
                                    {8,12},
                                    {9,13},
                                    {10,11,14,15},
                                    {11,14,15},
                                    null,
                                    null,
                                    {14,15}};
    public short[] AuxilaryArray=null;  
    public short Length;
    
    public Order(short length,StringNumber PasswordCode){
        this.Length=length;
        this.AuxilaryArray=new short[this.Length-1];
        if(!PasswordCode.EqualsTo(new StringNumber())){
            byte k;
            StringNumber x;
            if(this.Length==16){
                k=0;
                x=new Division(PasswordCode,InputParameters.MaxScales[k]).Rest;
                byte l=(byte)InputParameters.Scales[k].length;
                l--;
                for(short i=(short)(this.AuxilaryArray.length-1);i>-1;i--){
                    if(Order.SecretMatrix[i]==null)
                        continue;
                    Division d=new Division(x,InputParameters.Scales[k][l]);
                    this.AuxilaryArray[i]=(short)(Order.SecretMatrix[i][(short)d.Quotient.toInteger()]-i);
                    x=d.Rest;
                    l--;
                }
            }
            else{
                k=1;
                x=new Division(PasswordCode,InputParameters.MaxScales[k]).Rest;
                for(int i=this.AuxilaryArray.length-1;i>-1;i--){
                    Division d=new Division(x,InputParameters.Scales[k][i]);
                    this.AuxilaryArray[i]=(short)d.Quotient.toInteger();
                    x=d.Rest;
                }
            }
        }
    }  
    
    public Order(){
        this.Length=InputParameters._256;
        if(!InputParameters.DefaultCode.EqualsTo(new StringNumber())){
            this.AuxilaryArray=new short[this.Length-1];
            StringNumber x=InputParameters.DefaultCode.clone();
            this.AuxilaryArray=new short[this.Length-1];
            for(int i=this.AuxilaryArray.length-1;i>-1;i--){
                Division d=new Division(x,InputParameters.Scales[1][i]);
                this.AuxilaryArray[i]=(short)d.Quotient.toInteger();
                x=d.Rest;
            }
        }
    }
    
    public short[] getOrder(){
        short[] order=null;
        if(this.AuxilaryArray!=null){
            if(this.Length==16)
                order=Order.DefaultOrder.clone();
            else{
                order=new short[InputParameters._256];
                for(short i=0;i<order.length;i++)
                    order[i]=i;
                for(short i=0;i<InputParameters.DefaultOrder.AuxilaryArray.length;i++)
                    new ExchangeMovement(i,i+InputParameters.DefaultOrder.AuxilaryArray[i]).Execute(order);
            }
            for(short i=0;i<this.AuxilaryArray.length;i++)
                new ExchangeMovement(i,i+this.AuxilaryArray[i]).Execute(order);
        }
        return order;
    } 
    
    public static short[] Inverse(short[] array){
        short[] t=new short[array.length];
        IntStream.range(0,array.length)
                .forEach(i->t[i]=(short)IntStream.range(0,array.length).reduce(-1,(k,l)->(k==-1 && array[l]==i)?l:k));
        return t;
    }
}