export class ApinaConfig {

    /** Prefix added for all API calls */
    baseUrl: string = "";

    private serializers: SerializerFactoryMap = {
        any: (_env: TypeEnvironment) => identitySerializer,
        string: (_env: TypeEnvironment) => identitySerializer,
        number: (_env: TypeEnvironment) => identitySerializer,
        boolean: (_env: TypeEnvironment) => identitySerializer
    };

    private parameterizedSerializers: ParameterizedSerializerFactoryMap = {};

    constructor() {
        registerDefaultSerializers(this);
        this.registerSerializerFactory('Array<V>', (env: TypeEnvironment) => this.arraySerializer(env));
    }

    serialize(value: any, type: string): any {
        return this.lookupSerializer(type).serialize(value);
    }

    deserialize(value: any, type: string): any {
        return this.lookupSerializer(type).deserialize(value);
    }

    registerSerializerFactory(name: string, serializerFactory: SerializerFactory) {
        const {baseType, args} = parseType(name);

        if (args.length === 0) {
            this.serializers[name] = serializerFactory;
        } else {
            this.parameterizedSerializers[baseType] = {serializerFactory: serializerFactory, typeArgs: args};
        }
    }

    registerSerializer(name: string, serializer: Serializer) {
        this.registerSerializerFactory(name, (_env: TypeEnvironment) => serializer);
    }

    registerEnumSerializer(name: string, enumObject: any) {
        this.registerSerializer(name, enumSerializer(enumObject));
    }

    registerClassSerializer(name: string, fields: any) {
        this.registerSerializerFactory(name, (env: TypeEnvironment) => this.classSerializer(fields, env));
    }

    registerIdentitySerializer(name: string) {
        this.registerSerializer(name, identitySerializer);
    }

    private lookupParameterizedSerializer(type: string): Serializer | null {

        const {baseType, args} = parseType(type);
        const serializerFactoryWithTypes = this.parameterizedSerializers[baseType];

        if (!serializerFactoryWithTypes) {
            return null;
        }

        const {serializerFactory, typeArgs} = serializerFactoryWithTypes;

        const env: TypeEnvironment = {};

        for (let i = 0; i < typeArgs.length; i++) {
            env[typeArgs[i]] = args[i];
        }

        return {
            serialize(o: any): any {
                return serializerFactory(env).serialize(o)
            },
            deserialize(o: any): any {
                return serializerFactory(env).deserialize(o)
            }
        }
    }

    private lookupSerializer(type: string): Serializer {
        if (!type) throw new Error("no type given");

        if (type.indexOf('[]', type.length - 2) !== -1) { // type.endsWith('[]')
            const elementType = type.substring(0, type.length - 2);
            return this.lookupParameterizedSerializer(`Array<${elementType}>`)
        }

        const serializerFactory = this.serializers[type];

        if (serializerFactory) {
            return serializerFactory({});
        }

        const parameterizedSerializer = this.lookupParameterizedSerializer(type);

        if (parameterizedSerializer) {
            return parameterizedSerializer;
        } else {
            throw new Error(`could not find serializer for type '${type}'`);
        }
    }

    private classSerializer(fields: any, env: TypeEnvironment): Serializer {
        function mapProperties(obj: any, propertyMapper: (value: any, type: string) => any) {
            if (obj === null || obj === undefined) {
                return obj;
            }

            const result: any = {};

            for (const name in fields) {
                if (fields.hasOwnProperty(name)) {
                    const value: any = obj[name];
                    const type: string = fields[name];
                    result[name] = propertyMapper(value, type);
                }
            }

            return result;
        }

        const serialize = (value: any, type: string) => {
            return this.lookupSerializer(substituteEnvironment(type, env)).serialize(value);
        };

        const deserialize = (value: any, type: string) => {
            return this.lookupSerializer(substituteEnvironment(type, env)).deserialize(value);
        };

        return {
            serialize(obj) {
                return mapProperties(obj, serialize);
            },
            deserialize(obj) {
                return mapProperties(obj, deserialize);
            }
        };
    }

    private arraySerializer(env: TypeEnvironment): Serializer {
        function safeMap(value: any[], mapper: (a: any) => any) {
            if (!value)
                return value;
            else
                return value.map(mapper);
        }

        const serialize = (value: any) => {
            return this.lookupSerializer(env['V']).serialize(value);
        };

        const deserialize = (value: any) => {
            return this.lookupSerializer(env['V']).deserialize(value);
        };

        return {
            serialize(value) {
                return safeMap(value, serialize);
            },
            deserialize(value) {
                return safeMap(value, deserialize);
            }
        }
    }
}


export interface RequestData {
    uriTemplate: string;
    method: string;
    pathVariables?: any;
    requestParams?: any;
    requestBody?: any;
    responseType?: string;
}

type TypeEnvironment = { [type: string]: string }

export interface Serializer {
    serialize(o: any): any;
    deserialize(o: any): any;
}

export interface SerializerFactory {
    (env: TypeEnvironment): Serializer;
}

const identitySerializer: Serializer = {
    serialize(o) {
        return o;
    },
    deserialize(o) {
        return o;
    }
};

function substituteEnvironment(type: string, env: TypeEnvironment): string {
    let resultType = type;

    for (const [genericType, substitutionType] of Object.entries(env)) {

        if (resultType === genericType) {
            resultType = substitutionType
        }

        resultType = resultType.replace('<' + genericType + '>', '<' + substitutionType + '>');

        if (resultType.endsWith('[]')) {
            const arrayType = type.substring(0, type.indexOf('[]'));
            resultType = (env[arrayType] || arrayType) + '[]';
        }
    }

    return resultType;
}

function parseType(type: string): { baseType: string, args: string[] } {

    const start = type.indexOf('<');

    if (start !== -1) {
        const baseType = type.substring(0, start);
        const arg = type.substring(start + 1, type.indexOf('>'));

        return { baseType, args: [arg] };
    }

    return { baseType: type, args: [] };
}

function enumSerializer(enumObject: any): Serializer {
    return {
        serialize(o) {
            if (o === null || o === undefined)
                return o;
            else
                return enumObject[o];
        },
        deserialize(o) {
            if (o === null || o === undefined)
                return o;
            else
                return enumObject[o];
        }
    }
}

interface SerializerFactoryMap {
    [name: string]: SerializerFactory;
}

interface ParameterizedSerializerFactoryMap {
    [baseName: string]: { serializerFactory: SerializerFactory, typeArgs: string[] }
}

