/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ReferenceFactory;
import org.modeshape.jcr.value.ValueFactories;

/**
 * The {@link ValueFactory} implementation for ModeShape.
 */
public class JcrValueFactory implements org.modeshape.jcr.api.ValueFactory {

    private static final JcrValue[] EMPTY_ARRAY = new JcrValue[0];

    private final ValueFactories valueFactories;
    private final NamespaceRegistry namespaces;
    private final String executionContextProcessId;

    protected JcrValueFactory( ExecutionContext context ) {
        this.valueFactories = context.getValueFactories();
        this.namespaces = context.getNamespaceRegistry();
        this.executionContextProcessId = context.getProcessId();
    }

    public JcrValue[] createValues( List<?> values,
                                    int propertyType ) throws ValueFormatException {
        CheckArg.isNotNull(values, "values");
        final int size = values.size();
        if (size == 0) return EMPTY_ARRAY;
        JcrValue[] jcrValues = new JcrValue[size];
        int count = 0;
        org.modeshape.jcr.value.ValueFactory<?> valueFactory = valueFactoryFor(propertyType);
        for (Object value : values) {
            try {
                Object objectValue = valueFactory.create(value);
                jcrValues[count++] = new JcrValue(valueFactories, propertyType, objectValue);
            } catch (org.modeshape.jcr.value.ValueFormatException vfe) {
                throw new ValueFormatException(vfe);
            }
        }
        return jcrValues;
    }

    @Override
    public JcrValue createValue( String value,
                                 int propertyType ) throws ValueFormatException {
        if (value == null) return null;
        return new JcrValue(valueFactories, propertyType, convertValueToType(value, propertyType));
    }

    public JcrValue createValue( Object value,
                                 int propertyType ) throws ValueFormatException {
        if (value == null) return null;
        return new JcrValue(valueFactories, propertyType, convertValueToType(value, propertyType));
    }

    @Override
    public JcrValue createValue( Node value ) throws RepositoryException {
        if (value == null) {
            return new JcrValue(valueFactories, PropertyType.REFERENCE, null);
        }
        AbstractJcrNode node = validateReferenceableNode(value);
        Reference ref = valueFactories.getReferenceFactory().create(node.key(), node.isForeign());
        return new JcrValue(valueFactories, PropertyType.REFERENCE, ref);
    }

    @Override
    public JcrValue createValue( Node value,
                                 boolean weak ) throws RepositoryException {
        if (value == null) {
            return new JcrValue(valueFactories, weak ? PropertyType.WEAKREFERENCE : PropertyType.REFERENCE, null);
        }
        AbstractJcrNode node = validateReferenceableNode(value);
        ReferenceFactory factory = weak ? valueFactories.getWeakReferenceFactory() : valueFactories.getReferenceFactory();
        int refType = weak ? PropertyType.WEAKREFERENCE : PropertyType.REFERENCE;
        Reference ref = factory.create(node.key(), node.isForeign());
        return new JcrValue(valueFactories, refType, ref);
    }

    private AbstractJcrNode validateReferenceableNode( Node value ) throws RepositoryException {
        if (!value.isNodeType(JcrMixLexicon.REFERENCEABLE.getString(namespaces))) {
            throw new RepositoryException(JcrI18n.nodeNotReferenceable.text());
        }
        if (!(value instanceof AbstractJcrNode)) {
            throw new IllegalArgumentException("Invalid node type (expected a ModeShape node): " + value.getClass().toString());
        }

        AbstractJcrNode node = (AbstractJcrNode)value;
        if (!node.isInTheSameProcessAs(executionContextProcessId)) {
            throw new RepositoryException(JcrI18n.nodeNotInTheSameSession.text(node.path()));
        }
        return node;
    }

    @Override
    public JcrValue createValue( javax.jcr.Binary value ) {
        if (value == null) return null;
        return new JcrValue(valueFactories, PropertyType.BINARY, value);
    }

    @Override
    @SuppressWarnings("deprecation")
    public JcrValue createValue( InputStream value ) {
        if (value == null) return null;
        BinaryValue binary = valueFactories.getBinaryFactory().create(value);
        return new JcrValue(valueFactories, PropertyType.BINARY, binary);
    }

    @Override
    public BinaryValue createBinary( InputStream value ) {
        if (value == null) return null;
        return valueFactories.getBinaryFactory().create(value);
    }

    @Override
    public BinaryValue createBinary( InputStream value,
                                     String hint ) {
        if (value == null) return null;
        return valueFactories.getBinaryFactory().create(value, hint);
    }

    @Override
    public JcrValue createValue( Calendar value ) {
        if (value == null) return null;
        DateTime dateTime = valueFactories.getDateFactory().create(value);
        return new JcrValue(valueFactories, PropertyType.DATE, dateTime);
    }

    @Override
    public JcrValue createValue( boolean value ) {
        return new JcrValue(valueFactories, PropertyType.BOOLEAN, value);
    }

    @Override
    public JcrValue createValue( double value ) {
        return new JcrValue(valueFactories, PropertyType.DOUBLE, value);
    }

    @Override
    public JcrValue createValue( long value ) {
        return new JcrValue(valueFactories, PropertyType.LONG, value);
    }

    @Override
    public JcrValue createValue( String value ) {
        if (value == null) return null;
        return new JcrValue(valueFactories, PropertyType.STRING, value);
    }

    @Override
    public JcrValue createValue( BigDecimal value ) {
        if (value == null) return null;
        return new JcrValue(valueFactories, PropertyType.DECIMAL, value);
    }

    @Override
    public BinaryValue createBinary( byte[] value ) {
        if (value == null) return null;
        return valueFactories.getBinaryFactory().create(value);
    }

    @Override
    public JcrValue createValue( Date value ) {
        if (value == null) return null;
        return new JcrValue(valueFactories, PropertyType.DATE, value);
    }

    @Override
    public Value createValue( URI value ) {
        if (value == null) return null;
        return new JcrValue(valueFactories, PropertyType.URI, value);
    }

    public JcrValue createValue( Reference value ) {
        if (value == null) return null;
        int refType = value.isWeak() ? PropertyType.WEAKREFERENCE : PropertyType.REFERENCE;
        return new JcrValue(valueFactories, refType, value);
    }

    @Override
    public String createName( String localName ) {
        return valueFactories.getNameFactory().create((String)null, localName).getString();
    }

    @Override
    public String createName( String namespaceUri,
                              String localName ) {
        return valueFactories.getNameFactory().create(namespaceUri, localName).getString();
    }

    @Override
    public JcrValue createSimpleReference( Node node ) throws RepositoryException {
        AbstractJcrNode abstractJcrNode = validateReferenceableNode(node);
        Reference ref = valueFactories.getSimpleReferenceFactory().create(abstractJcrNode.key(), abstractJcrNode.isForeign());
        return new JcrValue(valueFactories, org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE, ref);
    }

    protected org.modeshape.jcr.value.ValueFactory<?> valueFactoryFor( int jcrPropertyType ) {
        switch (jcrPropertyType) {
            case PropertyType.BOOLEAN:
                return valueFactories.getBooleanFactory();
            case PropertyType.DATE:
                return valueFactories.getDateFactory();
            case PropertyType.NAME:
                return valueFactories.getNameFactory();
            case PropertyType.PATH:
                return valueFactories.getPathFactory();
            case PropertyType.REFERENCE:
                return valueFactories.getReferenceFactory();
            case PropertyType.WEAKREFERENCE:
                return valueFactories.getWeakReferenceFactory();
            case org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE:
                return valueFactories.getSimpleReferenceFactory();
            case PropertyType.DOUBLE:
                return valueFactories.getDoubleFactory();
            case PropertyType.LONG:
                return valueFactories.getLongFactory();
            case PropertyType.DECIMAL:
                return valueFactories.getDecimalFactory();
            case PropertyType.URI:
                return valueFactories.getUriFactory();

                // Anything can be converted to these types
            case PropertyType.BINARY:
                return valueFactories.getBinaryFactory();
            case PropertyType.STRING:
                return valueFactories.getStringFactory();
            case PropertyType.UNDEFINED:
                return valueFactories.getObjectFactory();
            default:
                assert false : "Unexpected JCR property type " + jcrPropertyType;
                // This should still throw an exception even if jcrPropertyType are turned off
                throw new IllegalStateException("Invalid property type " + jcrPropertyType);
        }

    }

    protected Object convertValueToType( Object value,
                                         int toType ) throws ValueFormatException {
        try {
            return valueFactoryFor(toType).create(value);
        } catch (org.modeshape.jcr.value.ValueFormatException vfe) {
            throw new ValueFormatException(vfe);
        }
    }

}
