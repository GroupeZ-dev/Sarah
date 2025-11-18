package fr.maxlego08.sarah;

import fr.maxlego08.sarah.database.Schema;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

public class ConsumerConstructor {


    /**
     * Creates a consumer from a template class that can be used to define a schema.
     * The consumer will be called with a {@link Schema} object and will define the columns
     * of the schema based on the fields of the template class.
     * <p>
     * The fields of the template class can have the following annotations:
     * <ul>
     *     <li>{@link Column}: defines the type, name and other properties of the column.
     *     If the type is not specified, it will be inferred from the field type.
     *     If the name is not specified, it will be the same as the field name.</li>
     * </ul>
     * <p>
     * The consumer can be used to define a schema for creating a table in a database.
     * <p>
     * The parameter <code>data</code> is an optional object that can be used to provide values
     * for the columns. If the object is not null, the consumer will use the values of the fields
     * of the object to define the columns of the schema.
     * <p>
     * @param template the template class
     * @param data an optional object that can be used to provide values for the columns
     * @return a consumer that can be used to define a schema
     */
    public static Consumer<Schema> createConsumerFromTemplate(Class<?> template, Object data) {
        Constructor<?>[] constructors = template.getDeclaredConstructors();
        Constructor<?> firstConstructor = constructors[0];
        firstConstructor.setAccessible(true);

        Field[] fields = template.getDeclaredFields();
        if (fields.length != firstConstructor.getParameterCount()) {
            throw new IllegalArgumentException("Fields count does not match constructor parameters count");
        }


        return schema -> {
            boolean primaryAlready = false;
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                field.setAccessible(true);
                Type type = field.getType();
                String name = field.getName();
                String typeName = type.getTypeName().substring(type.getTypeName().lastIndexOf('.') + 1);
                Column column = null;

                if (field.isAnnotationPresent(Column.class)) {
                    column = field.getAnnotation(Column.class);
                }

                if (column != null && !column.type().isEmpty()) {
                    typeName = column.type();
                }

                if (column != null && !column.value().isEmpty()) {
                    name = column.value();
                }

                if (column != null && column.autoIncrement()) {
                    if (type.equals(long.class) || type.equals(Long.class)) {
                        schema.autoIncrementBigInt(column.value());
                        primaryAlready = true;
                    } else if (type.equals(int.class) || type.equals(Integer.class)) {
                        schema.autoIncrement(column.value());
                        primaryAlready = true;
                    } else {
                        throw new IllegalArgumentException("Auto increment is only supported for long and int types");
                    }
                } else {
                    try {
                        schemaFromType(schema, typeName, name, data == null ? null : field.get(data));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (column != null) {
                    if(column.primary() && column.autoIncrement()) {
                        throw new IllegalArgumentException("A column cannot be both primary and auto increment");
                    }
                    if (column.primary()) {
                        primaryAlready = true;
                        schema.primary();
                    }
                    if (column.foreignKey()) {
                        if (column.foreignKeyReference().isEmpty()) {
                            throw new IllegalArgumentException("Foreign key reference is empty");
                        }
                        schema.foreignKey(column.foreignKeyReference());
                    }
                    if (column.nullable()) {
                        schema.nullable();
                    }
                }

                if (i == 0 && !primaryAlready) {
                    primaryAlready = true;
                    schema.primary();
                }
            }
        };
    }

    /**
     * Converts a type to a schema column.
     * <p>
     * The type is converted to a column in the schema as follows:
     * <ul>
     *     <li>{@code string}: a string column with a length of 255 characters if the object is null, otherwise a string column with the length of the object.toString()</li>
     *     <li>{@code longtext}: a long text column</li>
     *     <li>{@code integer}, {@code int}, {@code long}, {@code bigint}: a big int column if the object is null, otherwise a big int column with the value of the object as a long</li>
     *     <li>{@code boolean}: a boolean column if the object is null, otherwise a boolean column with the value of the object as a boolean</li>
     *     <li>{@code double}, {@code float}, {@code bigdecimal}: a decimal column if the object is null, otherwise a decimal column with the value of the object as a double</li>
     *     <li>{@code uuid}: a uuid column if the object is null, otherwise a uuid column with the value of the object as a uuid</li>
     *     <li>{@code date}: a date column with the value of the object as a date</li>
     *     <li>{@code timestamp}: a timestamp column with the value of the object as a date</li>
     * </ul>
     * <p>
     * If the type is not supported, an {@link IllegalArgumentException} is thrown.
     * <p>
     * @param schema the schema to add the column to
     * @param type the type of the column
     * @param name the name of the column
     * @param object the value of the column, or null if the column should be added without a value
     */
    private static void schemaFromType(Schema schema, String type, String name, Object object) {
        switch (type.toLowerCase()) {
            case "string":
                if (object == null) {
                    schema.string(name, 255);
                    break;
                }
                schema.string(name, object.toString());
                break;
            case "longtext":
                schema.longText(name);
                break;
            case "integer":
            case "int":
            case "long":
            case "bigint":
                if (object == null) {
                    schema.bigInt(name);
                    break;
                }
                schema.bigInt(name, Long.parseLong(object.toString()));
                break;
            case "boolean":
                if (object == null) {
                    schema.bool(name);
                    break;
                }
                schema.bool(name, (Boolean) object);
                break;
            case "double":
            case "float":
            case "bigdecimal":
                if (object == null) {
                    schema.decimal(name);
                    break;
                }
                schema.decimal(name, (Double) object);
                break;
            case "uuid":
                if (object == null) {
                    schema.uuid(name);
                    break;
                }
                schema.uuid(name, (UUID) object);
                break;
            case "date":
                schema.date(name, (Date) object).nullable();
                break;
            case "timestamp":
                schema.timestamp(name).nullable();
                break;
            default:
                throw new IllegalArgumentException("Type " + type + " is not supported");
        }
    }

}
