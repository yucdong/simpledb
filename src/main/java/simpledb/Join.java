package simpledb;

import java.util.*;

/**
 * The Join operator implements the relational join operation.
 */
public class Join extends Operator {

    private static final long serialVersionUID = 1L;

    private JoinPredicate pred;
    private OpIterator[] children;
    private Tuple curLeft;

    /**
     * Constructor. Accepts two children to join and the predicate to join them
     * on
     * 
     * @param p
     *            The predicate to use to join the children
     * @param child1
     *            Iterator for the left(outer) relation to join
     * @param child2
     *            Iterator for the right(inner) relation to join
     */
    public Join(JoinPredicate p, OpIterator child1, OpIterator child2) {
        this.pred = p;
        this.children = new OpIterator[2];
        this.children[0] = child1;
        this.children[1] = child2;
    }

    public JoinPredicate getJoinPredicate() {
        return this.pred;
    }

    /**
     * @return
     *       the field name of join field1. Should be quantified by
     *       alias or table name.
     * */
    public String getJoinField1Name() {
        OpIterator child1 = this.children[0];
        return child1.getTupleDesc().getFieldName(this.pred.getField1());
    }

    /**
     * @return
     *       the field name of join field2. Should be quantified by
     *       alias or table name.
     * */
    public String getJoinField2Name() {
        OpIterator child2 = this.children[1];
        return child2.getTupleDesc().getFieldName(this.pred.getField2());
    }

    /**
     * @see simpledb.TupleDesc#merge(TupleDesc, TupleDesc) for possible
     *      implementation logic.
     */
    public TupleDesc getTupleDesc() {
        return TupleDesc.merge(
                this.children[0].getTupleDesc(),
                this.children[1].getTupleDesc());
    }

    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        this.children[0].open();
        this.children[1].open();
        super.open();
        this.curLeft = this.children[0].next();
    }

    public void close() {
        super.close();
        this.children[0].close();
        this.children[1].close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        this.children[0].rewind();
        this.children[1].rewind();
        this.curLeft = this.children[0].next();
    }

    /**
     * Returns the next tuple generated by the join, or null if there are no
     * more tuples. Logically, this is the next tuple in r1 cross r2 that
     * satisfies the join predicate. There are many possible implementations;
     * the simplest is a nested loops join.
     * <p>
     * Note that the tuples returned from this particular implementation of Join
     * are simply the concatenation of joining tuples from the left and right
     * relation. Therefore, if an equality predicate is used there will be two
     * copies of the join attribute in the results. (Removing such duplicate
     * columns can be done with an additional projection operator if needed.)
     * <p>
     * For example, if one tuple is {1,2,3} and the other tuple is {1,5,6},
     * joined on equality of the first column, then this returns {1,2,3,1,5,6}.
     * 
     * @return The next matching tuple.
     * @see JoinPredicate#filter
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        Tuple t = null;
        OpIterator left = this.children[0];
        OpIterator right = this.children[1];

        // Assume this.curLeft already have a solid value
        try {
            while (t == null && this.curLeft != null) {
                while (right.hasNext()) {
                    Tuple r = right.next();
                    if (this.pred.filter(this.curLeft, r)) {
                        t = JoinTuples(this.curLeft, r);
                        break;
                    }
                }

                // Not match found on this round, rewind right
                if (t == null) {
                    this.curLeft = left.next();
                    right.rewind();
                }
            }
        } catch (NoSuchElementException ex) {
        }

        return t;
    }

    private Tuple JoinTuples(Tuple left, Tuple right) {
        Tuple t = new Tuple(this.getTupleDesc());
        for (int i = 0; i < left.getTupleDesc().numFields(); i++) {
            t.setField(i, left.getField(i));
        }

        for (int i = 0; i < right.getTupleDesc().numFields(); i++) {
            t.setField(i + left.getTupleDesc().numFields(), right.getField(i));
        }

        return t;
    }

    @Override
    public OpIterator[] getChildren() {
        return this.children;
    }

    @Override
    public void setChildren(OpIterator[] children) {
        this.children = children;
    }

}
