package software.amazon.awssdk.services.protocolrestxml.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.XmlAttributeTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class XmlNamespaceMember implements SdkPojo, Serializable,
        ToCopyableBuilder<XmlNamespaceMember.Builder, XmlNamespaceMember> {
    private static final SdkField<String> TYPE_FIELD = SdkField
            .<String> builder(MarshallingType.STRING)
            .memberName("Type")
            .getter(getter(XmlNamespaceMember::type))
            .setter(setter(Builder::type))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("foo:type")
                    .unmarshallLocationName("foo:type").build(), XmlAttributeTrait.create()).build();

    private static final SdkField<String> STRING_MEMBER_FIELD = SdkField
            .<String> builder(MarshallingType.STRING)
            .memberName("stringMember")
            .getter(getter(XmlNamespaceMember::stringMember))
            .setter(setter(Builder::stringMember))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("stringMember")
                    .unmarshallLocationName("stringMember").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(TYPE_FIELD,
            STRING_MEMBER_FIELD));

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = Collections
            .unmodifiableMap(new HashMap<String, SdkField<?>>() {
                {
                    put("foo:type", TYPE_FIELD);
                    put("stringMember", STRING_MEMBER_FIELD);
                }
            });

    private static final long serialVersionUID = 1L;

    private final String type;

    private final String stringMember;

    private XmlNamespaceMember(BuilderImpl builder) {
        this.type = builder.type;
        this.stringMember = builder.stringMember;
    }

    /**
     * Returns the value of the Type property for this object.
     * 
     * @return The value of the Type property for this object.
     */
    public final String type() {
        return type;
    }

    /**
     * Returns the value of the StringMember property for this object.
     * 
     * @return The value of the StringMember property for this object.
     */
    public final String stringMember() {
        return stringMember;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return BuilderImpl.class;
    }

    @Override
    public final int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(type());
        hashCode = 31 * hashCode + Objects.hashCode(stringMember());
        return hashCode;
    }

    @Override
    public final boolean equals(Object obj) {
        return equalsBySdkFields(obj);
    }

    @Override
    public final boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof XmlNamespaceMember)) {
            return false;
        }
        XmlNamespaceMember other = (XmlNamespaceMember) obj;
        return Objects.equals(type(), other.type()) && Objects.equals(stringMember(), other.stringMember());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("XmlNamespaceMember").add("Type", type()).add("StringMember", stringMember()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "Type":
            return Optional.ofNullable(clazz.cast(type()));
        case "stringMember":
            return Optional.ofNullable(clazz.cast(stringMember()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    @Override
    public final Map<String, SdkField<?>> sdkFieldNameToField() {
        return SDK_NAME_TO_FIELD;
    }

    private static <T> Function<Object, T> getter(Function<XmlNamespaceMember, T> g) {
        return obj -> g.apply((XmlNamespaceMember) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, XmlNamespaceMember> {
        /**
         * Sets the value of the Type property for this object.
         *
         * @param type
         *        The new value for the Type property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder type(String type);

        /**
         * Sets the value of the StringMember property for this object.
         *
         * @param stringMember
         *        The new value for the StringMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder stringMember(String stringMember);
    }

    static final class BuilderImpl implements Builder {
        private String type;

        private String stringMember;

        private BuilderImpl() {
        }

        private BuilderImpl(XmlNamespaceMember model) {
            type(model.type);
            stringMember(model.stringMember);
        }

        public final String getType() {
            return type;
        }

        public final void setType(String type) {
            this.type = type;
        }

        @Override
        public final Builder type(String type) {
            this.type = type;
            return this;
        }

        public final String getStringMember() {
            return stringMember;
        }

        public final void setStringMember(String stringMember) {
            this.stringMember = stringMember;
        }

        @Override
        public final Builder stringMember(String stringMember) {
            this.stringMember = stringMember;
            return this;
        }

        @Override
        public XmlNamespaceMember build() {
            return new XmlNamespaceMember(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }

        @Override
        public Map<String, SdkField<?>> sdkFieldNameToField() {
            return SDK_NAME_TO_FIELD;
        }
    }
}
