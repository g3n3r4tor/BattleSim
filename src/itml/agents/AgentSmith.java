package itml.agents;

import itml.cards.Card;
import itml.cards.CardDefend;
import itml.cards.CardRest;
import itml.simulator.CardDeck;
import itml.simulator.StateAgent;
import itml.simulator.StateBattle;
import weka.classifiers.Classifier;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;

import javax.swing.plaf.nimbus.State;
import java.util.ArrayList;

/**
 * Created by Lenovo on 28.9.2015.
 */
public class AgentSmith extends Agent {
    private int m_noThisAgent;     // Index of our agent (0 or 1).
    private int m_noOpponentAgent; // Index of opponent's agent.
    private Classifier classifier_;
    private Instances instances;
    public AgentSmith(CardDeck deck, int msConstruct, int msPerMove, int msLearn) {
        super(deck, msConstruct, msPerMove, msLearn);
        classifier_= new J48();
    }


    @Override
    public void startGame(int noThisAgent, StateBattle stateBattle) {
        // Remember the indicies of the agents in the StateBattle.
        m_noThisAgent = noThisAgent;
        m_noOpponentAgent  = (noThisAgent == 0 ) ? 1 : 0; // can assume only 2 agents battling.
    }

    @Override
    public void endGame(StateBattle stateBattle, double[] results) {

    }

    @Override
    public Card act(StateBattle stateBattle) {
        double[] values = new double[8];
        StateAgent a = stateBattle.getAgentState(m_noThisAgent);
        StateAgent o = stateBattle.getAgentState(m_noOpponentAgent);
        //System.out.println(stateBattle.toString());
        values[0] = o.getCol();
        values[1] = o.getRow();
        values[2] = o.getHealthPoints();
        values[3] = o.getStaminaPoints();
        values[4] = a.getCol();
        values[5] = a.getRow();
        values[6] = a.getHealthPoints();
        values[7] = a.getStaminaPoints();
        try {
            if(a.getStaminaPoints() == 0) {
                return new CardRest();
            }
            ArrayList<Card> allCards = m_deck.getCards();
            ArrayList<Card> cards = m_deck.getCards(a.getStaminaPoints());
            //System.out.println(classifier_.toString());
            Instance instance = new Instance(1.0, values.clone());
            instance.setDataset(instances);
            int out = (int)classifier_.classifyInstance(instance);

            Card selected = allCards.get(out);
            if(cards.contains(selected)) {
                //simulate opponenet's move.
                Card [] move = new Card[2];
                move[m_noOpponentAgent] = selected;

                //if opponent moves
                if(selected.getType() == Card.CardActionType.ctMove) {
                    // First check to see if we would be in attack range, if so attack.
                    int minDistance = calcDistanceBetweenAgents(stateBattle);
                    Card bestCard = new CardRest();
                    for ( Card card : cards ) {
                        StateBattle bs = (StateBattle) stateBattle.clone();
                        if (card.getType() == Card.CardActionType.ctAttack) {
                            int tmp = o.getHealthPoints();
                            move[m_noThisAgent] = card;
                            bs.play( move );
                            if(bs.getAgentState(m_noOpponentAgent).getHealthPoints() < tmp) {
                                return card;
                            }
                        }
                        else if (a.getStaminaPoints() < StateAgent.MAX_STAMINA) {
                            return new CardRest();

                        }
                        else {
                            move[m_noThisAgent] = card;
                            bs.play( move );
                            int  distance = calcDistanceBetweenAgents( bs );
                            if ( distance > minDistance ) {
                                bestCard = card;
                                minDistance = distance;
                            }
                        }

                    }
                    return bestCard;
                }

                if(selected.getType() == Card.CardActionType.ctAttack) {
                    Card bestCard = new CardRest();

                    for ( Card card : cards ) {
                        StateBattle bs = (StateBattle) stateBattle.clone();
                        if(selected.inAttackRange(o.getCol(), o.getRow() , a.getCol(), a.getRow())) {
                            if(a.getHealthPoints() <= o.getHealthPoints()) {
                                if (card.getType() == Card.CardActionType.ctMove) {
                                    int tmp = a.getHealthPoints();
                                    move[m_noThisAgent] = card;
                                    bs.play( move );
                                    if(bs.getAgentState(m_noThisAgent).getHealthPoints() == tmp) {
                                        bestCard = card;
                                    }

                                }
                                move[m_noThisAgent] = card;
                            }
                            else {
                                return selected;
                            }
                        }
                        else if(a.getStaminaPoints() < StateAgent.MAX_STAMINA){
                            return new CardRest();
                        }
                        else {
                            //maybe terminate mode here
                        }

                    }
                    if(bestCard.getName().equals("cRest")) {
                        return new CardDefend();
                    }
                    else {
                        return bestCard;
                    }
                }
                if(selected.getType() == Card.CardActionType.ctDefend) {
                    return new CardRest();
                }
                return new CardRest();
            }

        } catch (Exception e) {
            System.out.println("Error classifying new instance: " + e.toString());
        }
        return new CardRest();  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Classifier learn(Instances instances) {
        try {
            this.instances = instances;
            instances.setClassIndex(instances.numAttributes() - 1);
            //System.out.println(instances.toString());
            classifier_.buildClassifier(instances);
            //System.out.println(classifier_.toString());
        } catch(Exception e) {
            System.out.println("Error training classifier: " + e.toString());
        }
        return classifier_;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private int calcDistanceBetweenAgents( StateBattle bs ) {

        StateAgent asFirst = bs.getAgentState( 0 );
        StateAgent asSecond = bs.getAgentState( 1 );

        return Math.abs( asFirst.getCol() - asSecond.getCol() ) + Math.abs( asFirst.getRow() - asSecond.getRow() );
    }
}
