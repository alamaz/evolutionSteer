import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import processing.core.*;
import processing.opengl.*;
import processing.javafx.*;
import processing.data.*;

class Brain extends PApplet {
  float[][] neurons;
  Axon[][][] axons;
  int BRAIN_WIDTH = 0;
  int BRAIN_HEIGHT = 0;
  Brain(int bw, int bh, Axon[][][] templateAxons, Boolean haveNeurons, Boolean mutate){ //This is to copy a brain EXACTLY.
    setUpBasics(bw,bh,haveNeurons);
    axons = new Axon[BRAIN_WIDTH-1][BRAIN_HEIGHT][BRAIN_HEIGHT-1];
    if(mutate){
      for(int x = 0; x < BRAIN_WIDTH-1; x++){
        for(int y = 0; y < BRAIN_HEIGHT; y++){
          for(int z = 0; z < BRAIN_HEIGHT-1; z++){
            axons[x][y][z] = templateAxons[x][y][z].mutateAxon();
          }
        }
      }
    }else{
      for(int x = 0; x < BRAIN_WIDTH-1; x++){
        for(int y = 0; y < BRAIN_HEIGHT; y++){
          for(int z = 0; z < BRAIN_HEIGHT-1; z++){
        	    axons[x][y][z] = templateAxons[x][y][z].copyAxon(); 
          }
        }
      }
    }
  }
  Brain(int bw, int bh){
    setUpBasics(bw,bh,false);
    axons = new Axon[BRAIN_WIDTH-1][BRAIN_HEIGHT][BRAIN_HEIGHT-1];
    for(int x = 0; x < BRAIN_WIDTH-1; x++){
      for(int y = 0; y < BRAIN_HEIGHT; y++){
        for(int z = 0; z < BRAIN_HEIGHT-1; z++){
          double startingWeight = 0;
          if(y == BRAIN_HEIGHT-1){
            startingWeight = (Math.random()*2-1)*evolutionSteer.STARTING_AXON_VARIABILITY;
          }
          axons[x][y][z] = new Axon(startingWeight,evolutionSteer.AXON_START_MUTABILITY);
        }
      }
    }
  }
  void changeBrainStructure(int bw, int bh, int rowInsertionIndex, int rowRemovalIndex){
    setUpBasics(bw,bh,false);
    Axon[][][] oldAxons = axons;
    axons = new Axon[BRAIN_WIDTH-1][BRAIN_HEIGHT][BRAIN_HEIGHT-1];
    for(int x = 0; x < BRAIN_WIDTH-1; x++){
      for(int y = 0; y < BRAIN_HEIGHT; y++){
        for(int z = 0; z < BRAIN_HEIGHT-1; z++){
          if(y == rowInsertionIndex || z == rowInsertionIndex){
            double startingWeight = 0;
            if(y == BRAIN_HEIGHT-1 || true){
              startingWeight = (Math.random()*2-1)*evolutionSteer.STARTING_AXON_VARIABILITY;
            }
            axons[x][y][z] = new Axon(startingWeight,evolutionSteer.AXON_START_MUTABILITY);
          }else{
            int oldY = y;
            int oldZ = z;
            if(rowInsertionIndex >= 0 && y >= rowInsertionIndex) oldY--;
            if(rowInsertionIndex >= 0 && z >= rowInsertionIndex) oldZ--;
            if(rowRemovalIndex >= 0 && y >= rowRemovalIndex) oldY++;
            if(rowRemovalIndex >= 0 && z >= rowRemovalIndex) oldZ++;
            axons[x][y][z] = oldAxons[x][oldY][oldZ];
          }
        }
      }
    }
  }
  void setUpBasics(int bw, int bh, Boolean haveNeurons){
    BRAIN_WIDTH = bw;
    BRAIN_HEIGHT = bh;
    if(haveNeurons){
      neurons = new float[BRAIN_WIDTH][BRAIN_HEIGHT];
      for(int x = 0; x < BRAIN_WIDTH; x++){
        for(int y = 0; y < BRAIN_HEIGHT; y++){
          if(y == BRAIN_HEIGHT-1){
            neurons[x][y] = 1;
          }else{
            neurons[x][y] = 0;
          }
        }
      }
    }else{
      neurons = new float[BRAIN_WIDTH][BRAIN_HEIGHT];//null;
    }
  }
  public void useBrain(Creature owner){
    final ArrayList<Node> n = owner.n;
    final ArrayList<Muscle> m = owner.m;
    for(int i = 0; i < n.size(); i++){
      Node ni = n.get(i);
      neurons[0][i] = dist(ni.x, ni.y, ni.z, owner.foodX, owner.foodY, owner.foodZ);
    }
    for(int i = 0; i < m.size(); i++){
      Muscle am = m.get(i);
      Node ni1 = n.get(am.c1);
      Node ni2 = n.get(am.c2);
      neurons[0][n.size()+i] = dist(ni1.x, ni1.y, ni1.z, ni2.x, ni2.y, ni2.z)/am.len;
    }
    for(int x = 1; x < BRAIN_WIDTH; x++){
      for(int y = 0; y < BRAIN_HEIGHT-1; y++){
        float total = 0;
        for(int input = 0; input < BRAIN_HEIGHT; input++){
          total += neurons[x-1][input]*axons[x-1][input][y].weight;
        }
        if(x == BRAIN_WIDTH-1){
          neurons[x][y] = total;
        }else{
          neurons[x][y] = sigmoid(total);
        }
      }
    }
    for(int i = 0; i < m.size(); i++){
      m.get(i).brainOutput = neurons[BRAIN_WIDTH-1][n.size()+i];
    }
  }
  public float sigmoid(float input){
    //return 1.0/(1.0+pow((float)2.71828182846,-input)); 
    if(input >= -1 && input <= 1){ 
       return (float)0.25*input+(float)0.5; 
    } else if(input >= -3 && input < -1){ 
       return (float)0.105*input+(float)0.1192; 
    } else if(input > 1 && input <= 3){ 
       return (float)0.105*input+(float)0.8808; 
    } else if(input >= -5 && input < -3){ 
       return (float)0.0177*input+(float)0.018; 
    } else if(input > 3 && input <= 5){ 
       return (float)0.0177*input+(float)0.982; 
    } else if(input < -5){ 
       return 0; 
    } else { 
       return 1; 
    }  
  }
  Brain getUsableCopyOfBrain(){
    return new Brain(BRAIN_WIDTH,BRAIN_HEIGHT,axons.clone(),true,false);
  }
  Brain copyBrain(){
    return new Brain(BRAIN_WIDTH,BRAIN_HEIGHT,axons.clone(),false,false);
  }
  Brain copyMutatedBrain(){
    return new Brain(BRAIN_WIDTH,BRAIN_HEIGHT,axons.clone(),false,true);
  }
  Brain copyExpandedBrain(){ 
    Axon[][][] extaxons = new Axon[BRAIN_WIDTH][BRAIN_HEIGHT][BRAIN_HEIGHT-1]; 
    for(int x = 0; x < BRAIN_WIDTH; x++){ 
        for(int y = 0; y < BRAIN_HEIGHT; y++){ 
          for(int z = 0; z < BRAIN_HEIGHT-1; z++){ 
            if(x < BRAIN_WIDTH - 1){ extaxons[x][y][z] = axons[x][y][z].copyAxon(); } 
            if(x == BRAIN_WIDTH - 1){  
              if(y == z){ 
                extaxons[x][y][z] = new Axon(1.0,evolutionSteer.AXON_START_MUTABILITY); 
              } else { 
                extaxons[x][y][z] = new Axon(0.0,evolutionSteer.AXON_START_MUTABILITY); 
              } 
            } 
          } 
        } 
      } 
    return new Brain(BRAIN_WIDTH+1,BRAIN_HEIGHT,extaxons.clone(),false,false); 
  } 
  public void drawBrain(float scaleUp, Creature owner, evolutionSteer parent){
    ArrayList<Node> n = owner.n;
    ArrayList<Muscle> m = owner.m;
    final float neuronSize = (float)0.4;
    int abw = BRAIN_WIDTH*2-1;
    parent.noStroke();
    parent.fill(100);
    parent.rect(-neuronSize*2*scaleUp,-neuronSize*2*scaleUp,(abw+neuronSize*2)*scaleUp,(BRAIN_HEIGHT+neuronSize*2)*scaleUp);
    parent.fill(255);
    parent.rect(-neuronSize*3*scaleUp,-neuronSize*scaleUp,neuronSize*scaleUp,n.size()*scaleUp);
    parent.fill(0);
    parent.rect(-neuronSize*3*scaleUp,(n.size()-neuronSize)*scaleUp,neuronSize*scaleUp,m.size()*scaleUp);
    parent.ellipseMode(RADIUS);
    parent.strokeWeight((float) 0.5);
    parent.textAlign(CENTER);
    parent.textFont(evolutionSteer.font,(float)0.58*scaleUp);
    for(int x = 0; x < BRAIN_WIDTH; x++){
      for(int y = 0; y < BRAIN_HEIGHT; y++){
    	  	parent.noStroke();
        double val = neurons[x][y];
        parent.fill(neuronFillColor(val));
        parent.ellipse(x*2*scaleUp,y*scaleUp,neuronSize*scaleUp,neuronSize*scaleUp);
        parent.fill(neuronTextColor(val));
        parent.text(nf((float)val,0,1),x*2*scaleUp,(y+(neuronSize*(float)0.6))*scaleUp);
      }
    }
    for(int x = 0; x < BRAIN_WIDTH-1; x++){
      for(int y = 0; y < BRAIN_HEIGHT; y++){
        for(int z = 0; z < BRAIN_HEIGHT-1; z++){
          drawAxon(x,y,x+1,z,scaleUp,parent);
        }
      }
    }
  }
  public void drawAxon(int x1, int y1, int x2, int y2, float scaleUp,evolutionSteer parent){
    parent.stroke(neuronFillColor(axons[x1][y1][y2].weight*neurons[x1][y1]));
    parent.line(x1*2*scaleUp,y1*scaleUp,x2*2*scaleUp,y2*scaleUp);
  }
  public int neuronFillColor(double d){
    if(d >= 0){
      return color(255,255,255,(float)(d*255));
    }else{
      return color(1,1,1,abs((float)(d*255)));
    }
  }
  public int neuronTextColor(double d){
    if(d >= 0){
      return color(0,0,0);
    }else{
      return color(255,255,255);
    }
  }
  
  public void saveToJson(JsonGenerator g){
    try{ 
      g.writeNumberField("width", BRAIN_WIDTH);
      g.writeNumberField("height", BRAIN_HEIGHT);
      if(axons != null){
         g.writeArrayFieldStart("axons");
         for (int i = 0; i < axons.length; i++){
           g.writeStartArray();
           for(int j = 0; j < axons[i].length; j++){
             g.writeStartArray();
             for(int k = 0; k < axons[i][j].length; k++){
               g.writeStartObject(); axons[i][j][k].saveToJson(g); g.writeEndObject();
             }
             g.writeEndArray();
           }
           g.writeEndArray();
         }
         g.writeEndArray();
      }
      if(neurons != null){
         g.writeArrayFieldStart("neurons");
         for(int i = 0; i < neurons.length; i++){
           g.writeStartArray();
           for(int j = 0; j < neurons[i].length; j++){
             g.writeNumber(neurons[i][j]);
           }
           g.writeEndArray();
         }
         g.writeEndArray();
      }
    } catch(Exception e){
    		evolutionSteer.writeToErrorLog(e);
    }
  }
  
  public void loadFromJson(JsonParser p){
    try{
       while(p.nextToken() != JsonToken.END_OBJECT){
         String fieldName = p.getCurrentName();
         JsonToken token = p.nextToken();
         if(fieldName.equals("width")){ this.BRAIN_WIDTH = p.getIntValue(); }
         else if(fieldName.equals("height")){ this.BRAIN_HEIGHT = p.getIntValue(); }
         else if(fieldName.equals("axons")){
           if (token != JsonToken.START_ARRAY) { throw new Exception("Expected Array"); }
           int i = 0;
           axons = new Axon[BRAIN_WIDTH-1][BRAIN_HEIGHT][BRAIN_HEIGHT-1];
           while((token = p.nextToken()) != JsonToken.END_ARRAY){
             if (token == JsonToken.START_ARRAY){
               int j = 0;
               while((token = p.nextToken()) != JsonToken.END_ARRAY){
                 if (token == JsonToken.START_ARRAY){
                   int k = 0;
                   while((token = p.nextToken()) != JsonToken.END_ARRAY){
                     if (token != JsonToken.START_OBJECT) { throw new Exception("Expected Object"); }
                     axons[i][j][k] = new Axon(0,0);
                     axons[i][j][k].loadFromJson(p);
                     k += 1;
                   }
                 }
                 j += 1;
               }
             }
             i += 1;
           }
         }
         else if(fieldName.equals("neurons")){
           if (token != JsonToken.START_ARRAY) { throw new Exception("Expected Array"); }
           int i = 0;
           neurons = new float[BRAIN_WIDTH][BRAIN_HEIGHT];
           while((token = p.nextToken()) != JsonToken.END_ARRAY){
             if (token == JsonToken.START_ARRAY){
               int j = 0;
               while(p.nextToken() != JsonToken.END_ARRAY){
                 neurons[i][j] = p.getFloatValue();
                 j += 1;
               }
             }
             i += 1;
           }
         }
      }
    } catch(Exception e){
      evolutionSteer.writeToErrorLog(e);
    }
  }
  
}