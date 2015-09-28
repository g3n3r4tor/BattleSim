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

import java.util.ArrayList;

/**
 * User: deong
 * Date: 9/28/14
 */
public class LearningAgent extends Agent {
	private int m_noThisAgent;     // Index of our agent (0 or 1).
	private int m_noOpponentAgent; // Index of opponent's agent.
	private Classifier classifier_;
	private Instances instances;
	public LearningAgent( CardDeck deck, int msConstruct, int msPerMove, int msLearn ) {
		super(deck, msConstruct, msPerMove, msLearn);
		//classifier_ = new J48();
		classifier_ = new MultilayerPerceptron();
	}

	@Override
	public void startGame(int noThisAgent, StateBattle stateBattle) {
		// Remember the indicies of the agents in the StateBattle.
		m_noThisAgent = noThisAgent;
		m_noOpponentAgent  = (noThisAgent == 0 ) ? 1 : 0; // can assume only 2 agents battling.
	}

	@Override
	public void endGame(StateBattle stateBattle, double[] results) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Card act(StateBattle stateBattle) {
		double[] values = new double[8];
		StateAgent a = stateBattle.getAgentState(m_noThisAgent);
		StateAgent o = stateBattle.getAgentState(m_noOpponentAgent);
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
				if(selected.getType() == Card.CardActionType.ctMove) {
					return new CardRest();
				}
				if(selected.getType() == Card.CardActionType.ctDefend) {
					return new CardRest();
				}
				if(selected.getType() == Card.CardActionType.ctAttack) {
					return new CardDefend();
				}

				return selected;
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
}
