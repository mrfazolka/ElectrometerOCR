package my.nnpreproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.graphics.Bitmap;

/**
 *
 * @author "Ondøej Hanzlík"
 */

/**
Tøída pro získání atributù z obrázkù znakù
*/
public class CharAttributes {
    
    /**kolekce obrázkù znakù bez definujícího znaku*/
    ArrayList <Bitmap> listObr;
    /**pole atributù obrázkù znakù*/
    public ArrayList <int[][]> seznamPoliPrevodu = new ArrayList<int[][]>();
    public final int rozsah_x=4;
    public final int rozsah_y=4;
    public Bitmap im;
    public String znak;
    public boolean znakyVHashmap;
    /**hashmapa obrázkù znakù s definujícím znakem*/
    public HashMap<Bitmap, double[]> hashmap;
    /**nastavení prahu pro klasifikaci jasu pixelu*/
    final double prahSvetlosti = 0.7; // (0 - 255) 0-nejtmavší - ovlivòuje získání atributù z obrázku

    
    /**
     * @param list kolekce obrázkù znakù bez definujícího znaku
     * pøevede obrázky znakù v kolekci
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
     * @return vrátí kolekci polí atributù obrázkù
     */
    public ArrayList <int[][]> getSeznamPoliPrevodu() {
        return seznamPoliPrevodu;
    }


    /**
     * 
     * projde složku s obrázky, pøevede abrázek na 64 èíselných atributù
     @return vrátí hashmapu obrázkù znakù s definujícím znakem
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
     * naplni pole 8x8 atributy obrázku
     * první atribut je poèet èerných bodù v prvním ètverci prvního sloupce, druhý je druhý ètverec prvního sloupce, tøetí atr. - 1. sloupec 3. ètverec v nìm, ètvrtý atr. - 1 sloupec 4. ètverec
       =>(sloupce(x) x øádky(y)) 0x0, 0x1, 0x2, 0x3, ... 0x7, 1x0, 1x1, 1x2 ... 1x7
     *@return vrátí pole atributù obrázkù znakù
     */
    private int[][] naplnPoleAtributu()
    {
	int pole_prevod[][] = new int[8][8];
	int x=0;
	int y;
	for(int i=0; i<pole_prevod.length; i++) //projdi první rozmìr pole (øádky)
	{
	    y=0;
	    for(int j=0; j<pole_prevod[i].length; j++) //projdi druhý rozmìr pole (sloupce) - prvky uvnitø prvního rozmìru pole
	    {
	       pole_prevod[i][j]=blackCount(x,y); //zapis do aktuálního øádku x sloupce poèet èerných bodù na ploše o rozmìru rozsah_x x rozsah_y
	       y+=rozsah_y;
	    }
	    x+=rozsah_x;   //zvyš o poèátek procházené plochy o rozsah který procházím (posun na další procházenou èást)
	}

	return pole_prevod;
    }

    /**
     * spoète poèet tmavých pixelù v daném ètverci
     * @param zacatek_x x-ová souøadnice zaèátku procházeného ètverce
     * @param zacatek_y y-ová souøadnice zaèátku procházeného ètverce
     *@return vrátí poèet tmavých pixelù v procházeném ètverci
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
	                   
	                   if( tmavy(R, G, B) ) //když je svìtlost pod stanovenou hranicí (255 je max) pixel je vyhodnocen jako èerný
	                       cerne_body++;
                    }
               }
           }
        }

        return cerne_body;     // vrati poèet èerných bodù v oblasti velikosti rozsah_x x rozsah_y
    }
    
    /**
     * spoète poèet tmavých pixelù v daném ètverci
     * @param r èervená složka pixelu
     * @param g zelená složka pixelu
     * @param b modrá složka pixelu
     *@return vrátí true když je pixel tmavý, jinak false
     */
    private boolean tmavy(int r,int g, int b)
    {
	int y = (int)((0.299 * r) + (0.587 * g) + (0.114 * b)); //0 - 255; 0 - nejtmavší, 255 - nejsvìtlejší

        if(y < 255.0 * prahSvetlosti)  //hranice svìtlosti - stanovýme kdy je ještì bod považován za tmavý a kdy už ne
            return true;
        else
            return false;
    }

    /**
     * zapíše pole atributù obrázkù do souboru
     */
    public void zapisTxt(ArrayList<int[][]> list)
    {
        String pole_string="";
        int id_definovaneho_znaku=0;

        for(int[][] pole : list)
        {
            StringBuffer pole_stringbuffer = new StringBuffer();

            if(znak!=null)  //když je definovan znak jakému atributy náleží
            {
                if(znakyVHashmap) //když je sada definovaných znakù v hashmapì
                {
                    pole_stringbuffer.append(hashmap.get(listObr.get(id_definovaneho_znaku))+",");  //pøidá na zaèátek atributù znaku samotný atribut který k danému obrázku znaku patøí
                    id_definovaneho_znaku++;
                }
                else //když je definovaný znak jeden - v promìnné "znak"
                {
                    pole_stringbuffer.append(znak+",");
                }
            }
            else
            {
                pole_stringbuffer.append("unknown"+",");
            }

            for(int i=0; i<pole.length; i++) //procházení pole pøevedeného obrázku - poèet èerných bodù ve ètvercích 4x4 pixely
                for(int hodnota : pole[i])
                   pole_stringbuffer.append(hodnota+","); //z hodnot pole udìlá øetìzec StringBuffer - øetìzec, na kterém jdou použít rùzné modifikaèní fce
        
            
        pole_stringbuffer.setCharAt(pole_stringbuffer.length()-1, (char) 10); //jelikož na posledním místì øetìzce je "," pøepíšeme ji na linefeed(odøádkování) - char 10 - konec øetìzce
        
        pole_string += new String(pole_stringbuffer); //z StringBuffer udìlá String pro možnost zápisu do souboru
        }

        try {
            FileOutputStream soubor;
            if(znak==null)
            {
                soubor = new FileOutputStream("DATA/atributy.csv", true);  //param true - povolit dopisování na konec
                soubor.write(pole_string.getBytes());  //prevede pole na string a zapise do souboru (zapisované hodnoty se pøevedou na bytes - vezme se jejich ASCI hodnota proto, aby mohli být zapsány)
                soubor.close();
                System.out.println("Atributy znaku zapsany! (cesta: "+new File("DATA/atributy.csv").getAbsolutePath()+"\")");
            }
            else
            {
                soubor = new FileOutputStream("DATA/definovane.csv", true);  //param true - povolit dopisování na konec
                soubor.write(pole_string.getBytes());  //prevede pole na string a zapise do souboru (zapisované hodnoty se pøevedou na bytes - vezme se jejich ASCI hodnota proto, aby mohli být zapsány)
                soubor.close();
            }
        } catch (IOException ex) {
            System.out.println("Nepodarilo se zapsat hodnoty do souboru");
        }
    }
}