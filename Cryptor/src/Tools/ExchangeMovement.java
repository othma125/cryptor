/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Tools;

/**
 *
 * @author Othmane
 */
public class ExchangeMovement {
    private int index1;
    private int index2;
    public ExchangeMovement(int a,int b){
        index1=a;
        index2=b;
    }
    public void Execute(short[] ord){
        if(this!=null){
            if(index1<ord.length && index2<ord.length && index1!=index2){
               short aux;
               aux=ord[index1];
               ord[index1]=ord[index2];
               ord[index2]=aux;
            }
        }      
    }
}
