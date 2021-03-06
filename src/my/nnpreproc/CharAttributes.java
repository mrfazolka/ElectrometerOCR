package my.nnpreproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.graphics.Bitmap;

/**
 *
 * @author "Ond�ej Hanzl�k"
 */

/**
T��da pro z�sk�n� atribut� z obr�zk� znak�
*/
public class CharAttributes {
    
    /**kolekce obr�zk� znak� bez definuj�c�ho znaku*/
    ArrayList <Bitmap> listObr;
    /**pole atribut� obr�zk� znak�*/
    public ArrayList <int[][]> seznamPoliPrevodu = new ArrayList<int[][]>();
    public final int rozsah_x=4;
    public final int rozsah_y=4;
    public Bitmap im;
    public String znak;
    public boolean znakyVHashmap;
    /**hashmapa obr�zk� znak� s definuj�c�m znakem*/
    public HashMap<Bitmap, double[]> hashmap;
    /**nastaven� prahu pro klasifikaci jasu pixelu*/
    final double prahSvetlosti = 0.7; // (0 - 255) 0-nejtmav�� - ovliv�uje z�sk�n� atribut� z obr�zku

    
    /**
     * @param list kolekce obr�zk� znak� bez definuj�c�ho znaku
     * p�evede obr�zky znak� v kolekci
     */
    public CharAttributes(ArrayList <Bitmap> list)
    {
        listObr = list;
        projdiObjektyProPrevod(listObr);
//        System.out.println("Prevod obrazku znaku na atributy hotov!");
    }
    
    public CharAttributes(HashMap<Bitmap, double[]> h)
    {
        listObr = new ArrayList <Bitmap>();
        this.hashmap=h;
        znakyVHashmap=true;
        znak="ano";

        for(Bitmap obr : hashmap.keySet())
            listObr.add(obr);

        projdiObjektyProPrevod(listObr);
        System.out.println("Prevod obrazku znaku na atributy s definujicim znakem hotov!");
    }
/////////////////////
    /**
     * 
     * @return vr�t� kolekci pol� atribut� obr�zk�
     */
    public ArrayList <int[][]> getSeznamPoliPrevodu() {
        return seznamPoliPrevodu;
    }


    /**
     * 
     * projde slo�ku s obr�zky, p�evede abr�zek na 64 ��seln�ch atribut�
     @return vr�t� hashmapu obr�zk� znak� s definuj�c�m znakem
     */
    private ArrayList<int[][]> projdiObjektyProPrevod(ArrayList <Bitmap> list)
    {
	for(int i=0; i<list.size(); i++)
        { 
          im = list.get(i);
          seznamPoliPrevodu.add(naplnPoleAtributu());
        }
    
    return seznamPoliPrevodu;
    }

    /**
     * naplni pole 8x8 atributy obr�zku
     * prvn� atribut je po�et �ern�ch bod� v prvn�m �tverci prvn�ho sloupce, druh� je druh� �tverec prvn�ho sloupce, t�et� atr. - 1. sloupec 3. �tverec v n�m, �tvrt� atr. - 1 sloupec 4. �tverec
       =>(sloupce(x) x ��dky(y)) 0x0, 0x1, 0x2, 0x3, ... 0x7, 1x0, 1x1, 1x2 ... 1x7
     *@return vr�t� pole atribut� obr�zk� znak�
     */
    private int[][] naplnPoleAtributu()
    {
	int pole_prevod[][] = new int[8][8];
	int x=0;
	int y;
	for(int i=0; i<pole_prevod.length; i++) //projdi prvn� rozm�r pole (��dky)
	{
	    y=0;
	    for(int j=0; j<pole_prevod[i].length; j++) //projdi druh� rozm�r pole (sloupce) - prvky uvnit� prvn�ho rozm�ru pole
	    {
	       pole_prevod[i][j]=blackCount(x,y); //zapis do aktu�ln�ho ��dku x sloupce po�et �ern�ch bod� na plo�e o rozm�ru rozsah_x x rozsah_y
	       y+=rozsah_y;
	    }
	    x+=rozsah_x;   //zvy� o po��tek proch�zen� plochy o rozsah kter� proch�z�m (posun na dal�� proch�zenou ��st)
	}

	return pole_prevod;
    }

    /**
     * spo�te po�et tmav�ch pixel� v dan�m �tverci
     * @param zacatek_x x-ov� sou�adnice za��tku proch�zen�ho �tverce
     * @param zacatek_y y-ov� sou�adnice za��tku proch�zen�ho �tverce
     *@return vr�t� po�et tmav�ch pixel� v proch�zen�m �tverci
     */
    private int blackCount(int zacatek_x, int zacatek_y)
    {
    //Color b;
    int konec_x=zacatek_x+rozsah_x;
    int konec_y=zacatek_y+rozsah_y;
    int cerne_body=0;

        for(int osa_x = zacatek_x; osa_x < konec_x; osa_x++)
        {
           for (int osa_y = zacatek_y; osa_y < konec_y; osa_y++)
           {
              //Color b=new Color(im.getRGB(osa_x, osa_y));
        	   int picw = im.getWidth();
        	   int pich = im.getHeight();
        	   int[] pix = new int[picw * pich];
               im.getPixels(pix, 0, picw, 0, picw, 0, pich);
               for (int y = 0; y < pich; y++){
                   for (int x = 0; x < picw; x++)
                    {
                	   int index = y * picw + x;
                	   int R = (pix[index] >> 16) & 0xff; //bitwise shifting //int R = (p & 0xff0000) >> 16;
                	   int G = (pix[index] >> 8) & 0xff;
	                   int B = pix[index] & 0xff;
	
	                   //R,G.B - Red, Green, Blue
	                   //to restore the values after RGB modification, use 
	                   //next statement
	                   pix[index] = 0xff000000 | (R << 16) | (G << 8) | B;
	                   
	                   if( tmavy(R, G, B) ) //kdy� je sv�tlost pod stanovenou hranic� (255 je max) pixel je vyhodnocen jako �ern�
	                       cerne_body++;
                    }
               }
           }
        }

        return cerne_body;     // vrati po�et �ern�ch bod� v oblasti velikosti rozsah_x x rozsah_y
    }
    
    /**
     * spo�te po�et tmav�ch pixel� v dan�m �tverci
     * @param r �erven� slo�ka pixelu
     * @param g zelen� slo�ka pixelu
     * @param b modr� slo�ka pixelu
     *@return vr�t� true kdy� je pixel tmav�, jinak false
     */
    private boolean tmavy(int r,int g, int b)
    {
	int y = (int)((0.299 * r) + (0.587 * g) + (0.114 * b)); //0 - 255; 0 - nejtmav��, 255 - nejsv�tlej��

        if(y < 255.0 * prahSvetlosti)  //hranice sv�tlosti - stanov�me kdy je je�t� bod pova�ov�n za tmav� a kdy u� ne
            return true;
        else
            return false;
    }

    /**
     * zap�e pole atribut� obr�zk� do souboru
     */
    public void zapisTxt(ArrayList<int[][]> list)
    {
        String pole_string="";
        int id_definovaneho_znaku=0;

        for(int[][] pole : list)
        {
            StringBuffer pole_stringbuffer = new StringBuffer();

            if(znak!=null)  //kdy� je definovan znak jak�mu atributy n�le��
            {
                if(znakyVHashmap) //kdy� je sada definovan�ch znak� v hashmap�
                {
                    pole_stringbuffer.append(hashmap.get(listObr.get(id_definovaneho_znaku))+",");  //p�id� na za��tek atribut� znaku samotn� atribut kter� k dan�mu obr�zku znaku pat��
                    id_definovaneho_znaku++;
                }
                else //kdy� je definovan� znak jeden - v prom�nn� "znak"
                {
                    pole_stringbuffer.append(znak+",");
                }
            }
            else
            {
                pole_stringbuffer.append("unknown"+",");
            }

            for(int i=0; i<pole.length; i++) //proch�zen� pole p�eveden�ho obr�zku - po�et �ern�ch bod� ve �tverc�ch 4x4 pixely
                for(int hodnota : pole[i])
                   pole_stringbuffer.append(hodnota+","); //z hodnot pole ud�l� �et�zec StringBuffer - �et�zec, na kter�m jdou pou��t r�zn� modifika�n� fce
        
            
        pole_stringbuffer.setCharAt(pole_stringbuffer.length()-1, (char) 10); //jeliko� na posledn�m m�st� �et�zce je "," p�ep�eme ji na linefeed(od��dkov�n�) - char 10 - konec �et�zce
        
        pole_string += new String(pole_stringbuffer); //z StringBuffer ud�l� String pro mo�nost z�pisu do souboru
        }

        try {
            FileOutputStream soubor;
            if(znak==null)
            {
                soubor = new FileOutputStream("DATA/atributy.csv", true);  //param true - povolit dopisov�n� na konec
                soubor.write(pole_string.getBytes());  //prevede pole na string a zapise do souboru (zapisovan� hodnoty se p�evedou na bytes - vezme se jejich ASCI hodnota proto, aby mohli b�t zaps�ny)
                soubor.close();
                System.out.println("Atributy znaku zapsany! (cesta: "+new File("DATA/atributy.csv").getAbsolutePath()+"\")");
            }
            else
            {
                soubor = new FileOutputStream("DATA/definovane.csv", true);  //param true - povolit dopisov�n� na konec
                soubor.write(pole_string.getBytes());  //prevede pole na string a zapise do souboru (zapisovan� hodnoty se p�evedou na bytes - vezme se jejich ASCI hodnota proto, aby mohli b�t zaps�ny)
                soubor.close();
            }
        } catch (IOException ex) {
            System.out.println("Nepodarilo se zapsat hodnoty do souboru");
        }
    }
}