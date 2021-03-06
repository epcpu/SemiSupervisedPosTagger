package Tagging;

import Learning.AveragedPerceptron;
import Structures.*;

import java.util.ArrayList;
import java.util.TreeSet;

/**
 * Created by Mohammad Sadegh Rasooli.
 * ML-NLP Lab, Department of Computer Science, Columbia University
 * Date Created: 1/13/15
 * Time: 7:02 PM
 * To report any bugs or problems contact rasooli@cs.columbia.edu
 */

public class BeamTagger {

    public static int[] thirdOrder(final Sentence sentence, final AveragedPerceptron perceptron, final boolean isDecode, int beamWidth,final boolean usePartialInfo,final Tagger tagger){
        int len = sentence.words.length + 1;

        int tagSize = perceptron.tagSize();
        int featSize = perceptron.featureSize();

        ArrayList<Integer> allTags=new ArrayList<Integer>(tagSize-2);
        for(int i=2;i<tagSize;i++)
            allTags.add(i);

        // pai score values
        float emission_score[][] = new float[len - 1][tagSize];
        float bigramScore[][] = new float[tagSize][tagSize];
        float trigramScore[][][] = new float[tagSize][tagSize][tagSize];
        if(!isDecode) {
            for (int v = 0; v < tagSize; v++) {
                for (int u = 0; u < tagSize; u++) {
                    bigramScore[u][v] = perceptron.score(v, featSize - 2, u, isDecode);
                    for (int w = 0; w < tagSize; w++) {
                        int bigram = (w << 10) + u;
                        trigramScore[w][u][v] = perceptron.score(v, featSize -1, bigram, isDecode);
                    }
                }
            }
        }else{
            bigramScore=tagger.bigramScore;
            trigramScore=tagger.trigramScore;
        }

        for (int position = 0; position < sentence.words.length; position++) {
            int[] emissionFeatures = sentence.getEmissionFeatures(position, featSize);
            for (int t = 2; t < tagSize; t++) {
                emission_score[position][t] = perceptron.score(emissionFeatures, t, isDecode);
            }
        }

        ArrayList<TaggingState> beam=new ArrayList<TaggingState>();
        TaggingState initialState=new TaggingState(sentence.words.length);
        beam.add(initialState);

        for(int i=0;i<sentence.words.length;i++){
            TreeSet<BeamElement> elements=new TreeSet<BeamElement>();

            for(int b=0;b<beam.size();b++){
                TaggingState state= beam.get(b);
                int currentPosition=state.currentPosition;
                int prevTag=currentPosition>0?state.tags[currentPosition-1]:0;
                int prev2Tag=currentPosition>1?state.tags[currentPosition-2]:0;
                int prev3Tag=currentPosition>2?state.tags[currentPosition-3]:0;

                ArrayList<Integer> possibleTags=new ArrayList<Integer>();
                if(sentence.tags[i]==-1 || ! usePartialInfo)
                    possibleTags=allTags;
                else
                    possibleTags.add(sentence.tags[i]);

                for(int tagDecision : possibleTags) {
                    float es=emission_score[currentPosition][tagDecision];
                    float bs=bigramScore[prevTag][tagDecision];
                    float ts=trigramScore[prev2Tag][prevTag][tagDecision];
                    float score=es+bs+ts+state.score;
                    BeamElement element = new BeamElement(tagDecision,score,b);
                    elements.add(element);
                    if(elements.size()>beamWidth)
                        elements.pollFirst();
                }
            }

            ArrayList<TaggingState> newBeam=new ArrayList<TaggingState>();

            for(BeamElement element:elements){
                TaggingState state=beam.get(element.beamNum).clone();
                state.tags[state.currentPosition++]= element.tagDecision;
                state.score=element.score;
                newBeam.add(state);
            }
            beam=newBeam;
        }


        TreeSet<BeamElement> elements=new TreeSet<BeamElement>();
        for(int b=0;b<beam.size();b++) {
            TaggingState state = beam.get(b);
            int currentPosition = state.currentPosition;
            int prevTag = currentPosition > 0 ? state.tags[currentPosition - 1] : 0;
            int prev2Tag = currentPosition > 1 ? state.tags[currentPosition - 2] : 0;
            int prev3Tag = currentPosition > 2 ? state.tags[currentPosition - 3] : 0;
            int tagDecision = SpecialWords.stop.value;
            float bs = bigramScore[prevTag][tagDecision];
            float ts = trigramScore[prev2Tag][prevTag][tagDecision];
            float score = bs + ts + state.score;
            BeamElement element = new BeamElement(tagDecision, score, b);
            elements.add(element);
            if (elements.size() > beamWidth)
                elements.pollFirst();
        }

        int beamNum=elements.last().beamNum;
        return beam.get(beamNum).tags;
    }

    public static TaggingState thirdOrder(final Sentence sentence, final AveragedPerceptron perceptron, int beamWidth, UpdateMode updateMode) {
        int len = sentence.words.length + 1;
        int tagSize = perceptron.tagSize();
        int featSize = perceptron.featureSize();

        float maxViolation = Float.NEGATIVE_INFINITY;
        TaggingState maxViolState = new TaggingState(sentence.words.length);
        TaggingState goldState = new TaggingState(sentence.words.length);


        // pai score values
        float emission_score[][] = new float[len - 1][tagSize];
        float bigramScore[][] = new float[tagSize][tagSize];
        float trigramScore[][][] = new float[tagSize][tagSize][tagSize];

        for (int v = 0; v < tagSize; v++) {
            for (int u = 0; u < tagSize; u++) {
                bigramScore[u][v] = perceptron.score(v, featSize - 2, u, false);
                for (int w = 0; w < tagSize; w++) {
                    int bigram = (w << 10) + u;
                    trigramScore[w][u][v] = perceptron.score(v, featSize - 1, bigram, false);
                }
            }
        }

        for (int position = 0; position < sentence.words.length; position++) {
            int[] emissionFeatures = sentence.getEmissionFeatures(position, featSize);
            for (int t = 2; t < tagSize; t++) {
                emission_score[position][t] = perceptron.score(emissionFeatures, t, false);
            }
        }

        ArrayList<TaggingState> beam = new ArrayList<TaggingState>();
        TaggingState initialState = new TaggingState(sentence.words.length);
        beam.add(initialState);

        for (int i = 0; i < sentence.words.length; i++) {
            TreeSet<BeamElement> elements = new TreeSet<BeamElement>();


            for (int b = 0; b < beam.size(); b++) {
                TaggingState state = beam.get(b);
                int currentPosition = state.currentPosition;
                int prevTag = currentPosition > 0 ? state.tags[currentPosition - 1] : 0;
                int prev2Tag = currentPosition > 1 ? state.tags[currentPosition - 2] : 0;

                for (int tagDecision = 2; tagDecision < tagSize; tagDecision++) {
                    float es = emission_score[currentPosition][tagDecision];
                    float bs = bigramScore[prevTag][tagDecision];
                    float ts = trigramScore[prev2Tag][prevTag][tagDecision];
                    float score = es + bs + ts + state.score;
                    BeamElement element = new BeamElement(tagDecision, score, b);
                    elements.add(element);
                    if (elements.size() > beamWidth)
                        elements.pollFirst();
                }
            }

            int prevTag = goldState.currentPosition > 0 ? goldState.tags[goldState.currentPosition - 1] : 0;
            int prev2Tag = goldState.currentPosition > 1 ? goldState.tags[goldState.currentPosition - 2] : 0;
            float es = emission_score[goldState.currentPosition][sentence.tags[goldState.currentPosition]];
            float bs = bigramScore[prevTag][goldState.tags[goldState.currentPosition]];
            float ts = trigramScore[prev2Tag][prevTag][sentence.tags[goldState.currentPosition]];
            float score = es + bs + ts + goldState.score;
            goldState.score = score;
            goldState.tags[goldState.currentPosition] = goldState.tags[goldState.currentPosition];
            goldState.currentPosition++;

            ArrayList<TaggingState> newBeam = new ArrayList<TaggingState>();

            boolean oracleInBeam = false;
            for (BeamElement element : elements) {
                TaggingState state = beam.get(element.beamNum).clone();
                state.tags[state.currentPosition++] = element.tagDecision;
                state.score = element.score;
                newBeam.add(state);
                if (updateMode.value != updateMode.standard.value && !oracleInBeam) {
                    boolean same = true;
                    for (int j = 0; j <= state.currentPosition; j++) {
                        if (sentence.tags[i] != state.tags[i]) {
                            same = false;
                            break;
                        }
                    }
                    if (same)
                        oracleInBeam = true;
                }
            }

            if (updateMode.value != updateMode.standard.value && !oracleInBeam) {
                float viol = elements.last().score - goldState.score;
                if (viol > maxViolation) {
                    maxViolation = viol;
                    maxViolState = newBeam.get(newBeam.size() - 1);
                    if (updateMode.value == updateMode.early.value) {
                        return maxViolState;
                    }
                }
            }

            beam = newBeam;
        }


        TreeSet<BeamElement> elements = new TreeSet<BeamElement>();
        for (int b = 0; b < beam.size(); b++) {
            TaggingState state = beam.get(b);
            int currentPosition = state.currentPosition;
            int prevTag = currentPosition > 0 ? state.tags[currentPosition - 1] : 0;
            int prev2Tag = currentPosition > 1 ? state.tags[currentPosition - 2] : 0;
            int tagDecision = SpecialWords.stop.value;
            float bs = bigramScore[prevTag][tagDecision];
            float ts = trigramScore[prev2Tag][prevTag][tagDecision];
            float score = bs + ts + state.score;
            BeamElement element = new BeamElement(tagDecision, score, b);
            elements.add(element);
            if (elements.size() > beamWidth)
                elements.pollFirst();
        }

        int prevTag = goldState.currentPosition > 0 ? goldState.tags[goldState.currentPosition - 1] : 0;
        int prev2Tag = goldState.currentPosition > 1 ? goldState.tags[goldState.currentPosition - 2] : 0;
        float bs = bigramScore[prevTag][1];
        float ts = trigramScore[prev2Tag][prevTag][1];
        float score = bs + ts + goldState.score;
        goldState.score = score;


        int beamNum = elements.last().beamNum;
        TaggingState lastState = beam.get(beamNum);
        float viol = lastState.score - goldState.score;
        if (viol > maxViolation) {
            maxViolState = lastState;
        }

        if (updateMode.value != updateMode.maxViolation.value)
            return lastState;

        return maxViolState;
    }
}
