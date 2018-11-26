package simpledb;

import java.util.*;

/**
 * The Aggregation operator that computes an aggregate (e.g., sum, avg, max,
 * min). Note that we only support aggregates over a single column, grouped by a
 * single column.
 */
public class Aggregate extends Operator {

    private static final long serialVersionUID = 1L;

    private int aggField;
    private int gField;
    private Aggregator.Op aop;
    private OpIterator[] children;
    private Aggregator aggregator;
    private OpIterator iterator;

    /**
     * Constructor.
     * 
     * Implementation hint: depending on the type of afield, you will want to
     * construct an {@link IntegerAggregator} or {@link StringAggregator} to help
     * you with your implementation of readNext().
     * 
     * 
     * @param child
     *            The OpIterator that is feeding us tuples.
     * @param afield
     *            The column over which we are computing an aggregate.
     * @param gfield
     *            The column over which we are grouping the result, or -1 if
     *            there is no grouping
     * @param aop
     *            The aggregation operator to use
     */
    public Aggregate(OpIterator child, int afield, int gfield, Aggregator.Op aop) {
        this.aggField = afield;
        this.gField = gfield;
        this.aop = aop;
        this.children = new OpIterator[1];
        this.children[0] = child;
        this.iterator = null;
    }

    /**
     * @return If this aggregate is accompanied by a groupby, return the groupby
     *         field index in the <b>INPUT</b> tuples. If not, return
     *         {@link simpledb.Aggregator#NO_GROUPING}
     * */
    public int groupField() {
        return this.gField;
    }

    /**
     * @return If this aggregate is accompanied by a group by, return the name
     *         of the groupby field in the <b>OUTPUT</b> tuples. If not, return
     *         null;
     * */
    public String groupFieldName() {
        TupleDesc desc = this.children[0].getTupleDesc();
        if (this.gField != Aggregator.NO_GROUPING) {
            return desc.getFieldName(this.gField);
        }

        return null;
    }

    /**
     * @return the aggregate field
     * */
    public int aggregateField() {
        return this.aggField;
    }

    /**
     * @return return the name of the aggregate field in the <b>OUTPUT</b>
     *         tuples
     * */
    public String aggregateFieldName() {
        TupleDesc desc = this.children[0].getTupleDesc();
        return desc.getFieldName(this.aggField);
    }

    private Type aggregateFieldType() {
        TupleDesc desc = this.children[0].getTupleDesc();
        return desc.getFieldType(this.aggField);
    }

    private Type gbFieldType() {
        TupleDesc desc = this.children[0].getTupleDesc();
        return desc.getFieldType(this.gField);
    }

    /**
     * @return return the aggregate operator
     * */
    public Aggregator.Op aggregateOp() {
	    return this.aop;
    }

    public static String nameOfAggregatorOp(Aggregator.Op aop) {
	    return aop.toString();
    }

    private void calculateAggregation()
            throws NoSuchElementException,
                   DbException,
                   TransactionAbortedException {
        Type aggFieldType = this.aggregateFieldType();
        if (aggFieldType == Type.INT_TYPE) {
            this.aggregator = new IntegerAggregator(this.gField, this.gbFieldType(), this.aggField, this.aop);
        } else {
            this.aggregator = new StringAggregator(this.gField, this.gbFieldType(), this.aggField, this.aop);
        }

        while (this.children[0].hasNext()) {
            Tuple next = this.children[0].next();
            this.aggregator.mergeTupleIntoGroup(next);
        }
    }

    public void open() throws NoSuchElementException, DbException,
	    TransactionAbortedException {
        super.open();
        this.children[0].open();

        this.calculateAggregation();
        this.iterator = this.aggregator.iterator();
        this.iterator.open();
    }

    /**
     * Returns the next tuple. If there is a group by field, then the first
     * field is the field by which we are grouping, and the second field is the
     * result of computing the aggregate. If there is no group by field, then
     * the result tuple should contain one field representing the result of the
     * aggregate. Should return null if there are no more tuples.
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        if (this.iterator == null || !this.iterator.hasNext()) {
            return null;
        }

        return this.iterator.next();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // Rewind the iterator only, leaving the calculated result untouched
        this.iterator = this.aggregator.iterator();
        this.iterator.open();
    }

    /**
     * Returns the TupleDesc of this Aggregate. If there is no group by field,
     * this will have one field - the aggregate column. If there is a group by
     * field, the first field will be the group by field, and the second will be
     * the aggregate value column.
     * 
     * The name of an aggregate column should be informative. For example:
     * "aggName(aop) (child_td.getFieldName(afield))" where aop and afield are
     * given in the constructor, and child_td is the TupleDesc of the child
     * iterator.
     */
    public TupleDesc getTupleDesc() {
	    return this.children[0].getTupleDesc();
    }

    public void close() {
        this.children[0].close();
        this.iterator.close();
        super.close();
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
