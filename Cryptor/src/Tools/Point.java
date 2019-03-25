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
public class Point {
    public byte X,Y;
    public Point(byte k,byte n){
        this.X=(byte)(k/n);
        this.Y=(byte)(k%n);
    }
}
