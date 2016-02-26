/*  ----------------------------------------------------------------
    www.prometec.net
    Prog_72_1 : 
    Prometec.net Sesion 72, http://www.prometec.net/android-bt
    Basado en Ardudroid: http://www.techbitar.com/ardudroid-simple-bluetooth-control-for-arduino-and-android.html
    
    Controlando tu Arduino esde un movil Android con BlueTooth
--------------------------------------------------------------------  
*/
 

#include <SoftwareSerial.h>
SoftwareSerial BT1(3, 2); // RX | TX

#define START_CMD_CHAR '*'
#define END_CMD_CHAR '#'
#define DIV_CMD_CHAR '|'
#define CMD_DIGITALWRITE 10
#define CMD_ANALOGWRITE 11
#define CMD_TEXT 12
#define CMD_READ_ARDUDROID 13
#define MAX_COMMAND 20  // max command number code. used for error checking.
#define MIN_COMMAND 10  // minimum command number code. used for error checking. 
#define IN_STRING_LENGHT 40
#define MAX_ANALOGWRITE 255
#define PIN_HIGH 3
#define PIN_LOW 2

String inText;

void setup()
   { 
     Serial.begin(9600);
     BT1.begin(57600); 
   }

void loop()
   {  
/*     
     if (BT1.available())
            Serial.write(BT1.read());
      if (Serial.available())
            BT1.write(Serial.read());
return;             */
// ............................................
  Serial.flush();
  int ard_command = 0;
  int pin_num = 0;
  int pin_value = 0;

  char get_char = ' ';  // Para leer BT1
  if (BT1.available())
    {   get_char = BT1.read();
        //Serial.println(get_char);
        delay(25);
        
        if (get_char != START_CMD_CHAR)   
                return; // Si no hay comando, vuelta a empezar
    
        ard_command = BT1.parseInt(); // Leemos la orden
        pin_num = BT1.parseInt();     // Leemos el pin
        pin_value = BT1.parseInt();   // Leemos valor

        //Serial.print("Comando disponible ");       // Hay comandos disponibles        
        //Serial.println(ard_command);
  
        if (ard_command == CMD_TEXT)     // Si el comando es de texto:
          {   //processInText();
              String s = GetLine();
              Serial.println(s);
          } 
         if (ard_command == CMD_READ_ARDUDROID) 
             {   BT1.print(" Analog 0 = "); 
                 BT1.println(analogRead(A0));  // Leemos A0
                 return;  // Done. return to loop();   
             }
         if (ard_command == CMD_DIGITALWRITE)
             { processDW(pin_num, pin_value);
               return;
             }
             
        if (ard_command == CMD_ANALOGWRITE) 
           {  analogWrite(  pin_num, pin_value ); 
             // add your code here
             return;  // De vuelta alloop();
           }
    }
}

void processDW(int pin_num, int pin_value)
{   if (pin_value == PIN_LOW) 
        pin_value = LOW;
    else if (pin_value == PIN_HIGH)
        pin_value = HIGH;
    else 
        return; // error in pin value. return. 
        
    digitalWrite( pin_num,  pin_value);   
    return;  
}

String GetLine()
   {   String S = "" ;
       if (BT1.available())
          {    char c = BT1.read(); ;
                while ( c != END_CMD_CHAR)            //Hasta que el caracter sea END_CMD_CHAR
                  {     S = S + c ;
                        delay(25) ;
                        c = BT1.read();
                  }
                return( S ) ;
          }
   }
