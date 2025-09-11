class MPDataPoint {
    constructor(x, y) {
        this.x = x;
        this.y = Number(y);
    }
}

class MPPoint {
    constructor(x, y) {
        this.x = x === undefined ? 0 : Number(x);
        this.y = y === undefined ? 0 : Number(y);
    }
}

class MPState {
    static Normal = 0;
    static Warning = 1;
    static Alarm = 2;

    static NormalLabel = "Normal";
    static WarningLabel = "Warning";
    static AlarmLabel = "Alarm";
    static UnknownLabel = "Unknown";

    constructor(value) {
        this.value = value === undefined ? 0 : Number(value);
    }

    static getText(state) {
        switch (state) {
            case MPState.Normal:
                return MPState.NormalLabel;
            case MPState.Warning:
                return MPState.WarningLabel;
            case MPState.Alarm:
                return MPState.AlarmLabel;
            default:
                return MPState.UnknownLabel;
        }
    }

    toString() {
        return MPState.getText(this.value);
    }
}

class MPBinaryConverter {
    static epochOffset = BigInt(621355968000000000);
    static ticksPerMillisecond = BigInt(10000);
    static timeZoneOffsetMillisecond = BigInt(new Date().getTimezoneOffset() * 60000)
    static base64ToArrayBuffer(base64) {
        const binaryString = atob(base64);

        const length = binaryString.length;
        const bytes = new Uint8Array(length);

        for (let i = 0; i < length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }

        return bytes.buffer;
    }
    static base64ToUint8Array(value) {
        return new Uint8Array(MPBinaryConverter.base64ToArrayBuffer(value));
    }
    static ticksToDate(ticks) {
        const milliseconds = (ticks - MPBinaryConverter.epochOffset) / MPBinaryConverter.ticksPerMillisecond + MPBinaryConverter.timeZoneOffsetMillisecond;
        return new Date(Number(milliseconds));
    }
    static * dataPointsFromBase64(value) {
        var bytes = MPBinaryConverter.base64ToUint8Array(value);

        var number = new ArrayBuffer(4);
        var float32 = new Int8Array(number);
        var floats = new Float32Array(number);
        var x = BigInt(0);
        for (var i = 0; i < bytes.length; i += 12) {

            var x =
                (BigInt(bytes[i + 7]) << BigInt(56)) +
                (BigInt(bytes[i + 6]) << BigInt(48)) +
                (BigInt(bytes[i + 5]) << BigInt(40)) +
                (BigInt(bytes[i + 4]) << BigInt(32)) +
                (BigInt(bytes[i + 3]) << BigInt(24)) +
                (BigInt(bytes[i + 2]) << BigInt(16)) +
                (BigInt(bytes[i + 1]) << BigInt(8)) +
                (BigInt(bytes[i + 0]));

            float32[3] = bytes[i + 11];
            float32[2] = bytes[i + 10];
            float32[1] = bytes[i + 9];
            float32[0] = bytes[i + 8];
            yield new MPDataPoint(MPBinaryConverter.ticksToDate(x), floats[0]);
        }
    }
}

class ExtendedSVGSensor {
    static FunctionDefault = 0;
    static FunctionState = 1;
    constructor(element, x, y, key, tag, title, func) {
        this.x = Number(x);
        this.y = Number(y);
        this.key = key == null ? null : String(key);
        this.tag = tag == null ? null : String(tag);
        this.title = title == null ? null : String(title);
        this.func = func == null ? ExtendedSVGSensor.FunctionDefault : Number(func);
        this.element = element;
        this.binding = null;
        this.displaySettings = null;
        this.values = new Map();
        this.value = NaN;
        this.state = -1;
        this.connected = true;
        this.unitTag = null;
    }

    async bindTestPoint(client) {
        try {
            this.binding = await client.getTestPointByKeyAsync(this.key);
            if (this.binding == null) return;
            this.unitTag = MPTag.getUnitTag(client.tags, this.tag);
        }
        catch (e) {
            console.log(e);
        }
    }

    * getTags() {
        if (this.tag !== null) {
            yield this.tag;
            if (this.displaySettings == null && this.unitTag != null) {
                yield this.unitTag.key;
                yield MPTag.SensorType;
            }
            yield MPTag.ConnectionState;
            yield MPTag.TestpointState;
        }
    }

    hasBinding() {
        return this.binding != null;
    }

    update(updatedCallback) {
        if (this.binding == null) return;

        if (this.displaySettings == null && this.unitTag != null) {
            var sensor = this.values.get(MPTag.SensorType);
            var units = this.values.get(this.unitTag.key);
            if (sensor != null && units != null) {
                var settings = MPDisplaySettings.get(sensor.toNumber(), units.toNumber());
                if (settings != null) {
                    this.displaySettings = settings;
                }
            }
        }

        var value = this.values.get(this.tag);
        var state = this.values.get(MPTag.TestpointState);
        var connected = this.values.get(MPTag.ConnectionState);

        if (state != null) {
            this.state = state.toNumber();
        }
        if (connected != null) {
            this.connected = Boolean(connected.toNumber());
        }
        if (value != null) {
            this.value = value.toNumber();
        }

        if (updatedCallback != null)
            updatedCallback(this);
    }

    getValueText() {
        var value = this.values.get(this.tag);
        if (value == null) return null;
        if (this.displaySettings == null) {
            var units = "";
            if (value?.tag?.units != null)
                units = value.tag.units;

            return Math.round(((this.value) + Number.EPSILON) * 100) / 100 + units;
        }
        else
            return this.displaySettings.getValueText(this.value)
    }

    getValueTime() {
        var value = this.values.get(this.tag);
        var state = this.values.get(MPTag.ConnectionState);
        if (state?.dt != null) {
            return state.dt;
        }
        if (value?.dt != null) {
            return value.dt;
        }
        return null;
    }

    toString() {
        return this.getValueText() + ", state: " + MPState.getText(this.state) + ", connected: " + this.connected;
    }
}

class ExtendedSVG {

    static AttCX = "data-glb-cx";
    static AttCY = "data-glb-cy";
    static AttTag = "data-glb-tag";
    static AttId = "data-glb-id";
    static AttFunction = "data-glb-f";
    static AttTitle = "data-glb-title";
    static AttX = "data-glb-x";
    static AttY = "data-glb-y";

    constructor(xml) {
        var parser = new DOMParser();
        this.document = parser.parseFromString(xml, "image/svg+xml");
        this.sensors = [];
        for (const e of this.document.getElementsByTagName('*')) {
            const x = e.getAttribute(ExtendedSVG.AttX);
            const y = e.getAttribute(ExtendedSVG.AttY);
            if (x == null || y == null)
                continue;
            this.sensors.push(new ExtendedSVGSensor(
                e,
                x,
                y,
                e.getAttribute(ExtendedSVG.AttId),
                e.getAttribute(ExtendedSVG.AttTag),
                e.getAttribute(ExtendedSVG.AttTitle),
                e.getAttribute(ExtendedSVG.AttFunction)
            ));

        }
    }

    static fromUint8Array(uint8Array) {
        const decoder = new TextDecoder('utf-8');
        return new ExtendedSVG(decoder.decode(uint8Array));
    }

    async updateTestPointInformationAsync(client) {
        var promises = [];
        for (const s of this.sensors) {
            if (s.binding == null) {
                promises.push(s.bindTestPoint(client));
            }
        }
        await Promise.all(promises);
    }

    async updateValuesAsync(client, updatedCallback = null) {
        var tags = new Set();
        var ids = new Set();
        var map = new Map();
        for (const s of this.sensors) {
            if (s.binding != null) {
                map.set(s.binding.id, s);
                ids.add(s.binding.id);
                for (const t of s.getTags()) {
                    tags.add(t);
                }
            }
        }
        for await (const v of client.getMultipleTestPointDataAsync(ids, tags)) {
            if (v.testPointId?.id != null) {
                var tp = map.get(v.testPointId?.id);
                if (tp != null) {
                    tp.values.set(v.tag.key, v);
                }

            }
        }
        for (const s of map) {
            s[1].update(updatedCallback);
        }
    }
}

class MPAmplitudeUnit {
    static Other = 0;
    static dB = 0x01;
    static dBm = 0x02;
    static dBmV = 0x03;
    static dBuV = 0x04;
    static V = 0x05;
    static mV = 0x06;
    static uV = 0x07;
    static percent = 0x08;
    static A = 0x09;
    static mA = 0x0A;
    static uA = 0x0B;
    static Ohm = 0x0C;
    static mOhm = 0x0D;
    static uOhm = 0x0E;
    static mPerSqrSecond = 0x0F;
    static mm = 0x10;
    static degreeC = 0x11;
    static degreeF = 0x12;
    static Pa = 0x13;
    static C = 0x14;
    static mC = 0x15;
    static uC = 0x16;
    static nC = 0x17;
    static pC = 0x18;

    static getText(value) {
        switch (value) {
            case MPAmplitudeUnit.dB: return "dB";
            case MPAmplitudeUnit.dBm: return "dBm";
            case MPAmplitudeUnit.dBmV: return "dBmV";
            case MPAmplitudeUnit.dBuV: return "dBuV";
            case MPAmplitudeUnit.V: return "V";
            case MPAmplitudeUnit.mV: return "mV";
            case MPAmplitudeUnit.uV: return "uV";
            case MPAmplitudeUnit.percent: return "%";
            case MPAmplitudeUnit.A: return "A";
            case MPAmplitudeUnit.mA: return "mA";
            case MPAmplitudeUnit.uA: return "uA";
            case MPAmplitudeUnit.Ohm: return "Ohm";
            case MPAmplitudeUnit.mOhm: return "mOhm";
            case MPAmplitudeUnit.mPerSqrSecond: return "m*s^2";
            case MPAmplitudeUnit.mm: return "mm";
            case MPAmplitudeUnit.degreeC: return "C°";
            case MPAmplitudeUnit.degreeF: return "F°";
            case MPAmplitudeUnit.Pa: return "Pa";
            case MPAmplitudeUnit.C: return "C";
            case MPAmplitudeUnit.mC: return "mC";
            case MPAmplitudeUnit.uC: return "uC";
            case MPAmplitudeUnit.nC: return "nC";
            case MPAmplitudeUnit.pC: return "pC";
            default: return "N/A";
        }
    }
    constructor(value) {
        this.value = value === undefined ? MPAmplitudeUnit.Other : Number(value);
    }

    toString() {
        return MPAmplitudeUnit.getText(this.value);
    }
}

class MPSensorType {
    static Undefined = 0;
    static UHF = 11;
    static RF = 12;
    static AE = 21;
    static AA = 22;
    static AcousticImaging = 23;
    static TEV = 31;
    static HF = 41;

    constructor(value) {
        this.value = value === undefined ? MPSensorType.Undefined : Number(value);
    }

    toString() {
        switch (this.value) {
            case MPSensorType.UHF: return "UHF";
            case MPSensorType.RF: return "RF";
            case MPSensorType.AE: return "AE";
            case MPSensorType.AA: return "AA";
            case MPSensorType.AcousticImaging: return "AcousticImaging";
            case MPSensorType.TEV: return "TEV";
            case MPSensorType.HF: return "HF";
            default: return "N/A";
        }
    }

}

class MPDisplaySettings {
    static displaySettingsMap = new Map();
    constructor(sensor, unit) {
        this.sensor = new MPSensorType(sensor);
        this.originalUnit = new MPAmplitudeUnit(unit);
        this.unit = new MPAmplitudeUnit(unit);
        this.amplitudeFactor = 1;
        switch (this.sensor.value) {
            case MPSensorType.HF:
                switch (this.originalUnit.value) {
                    case MPAmplitudeUnit.V:
                        {
                            this.amplitudeFactor = 1000;
                            this.unit = new MPAmplitudeUnit(MPAmplitudeUnit.mV);
                        }
                        break;
                }

                break;
        }
    }

    getValueText(value) {
        return Math.round(((value * this.amplitudeFactor) + Number.EPSILON) * 100) / 100 + " " + this.unit.toString();
    }

    static get(sensor, unit) {

        var unitMap = MPDisplaySettings.displaySettingsMap.get(sensor);

        if (unitMap === undefined) {
            unitMap = new Map();

            MPDisplaySettings.displaySettingsMap.set(sensor, unitMap);

        }

        var result = unitMap.get(unit);

        if (result === undefined) {
            result = new MPDisplaySettings(sensor, unit);
            unitMap.set(unit, result);

        }
        return result;
    }

    toString() {
        return this.sensor.toString() + "/" + this.unit.toString() + "/" + this.amplitudeFactor;

    }
}

class MPRequest {
    constructor(token, data = null) {
        this.token = token == null ? null : String(token);
        this.data = data;
    }
}

class MPIDJson {
    constructor(id) {
        this.id = String(id);
    }

    static getId(obj) {
        if (obj === undefined || obj === null)
            return null;
        switch (typeof obj) {
            case "string":
            case "number":
                return String(obj);
            default:
                var id = obj.id;
                if (id !== undefined)
                    return String(id);
                break;
        }
        return null;
    }

    static create(obj) {
        if (obj === undefined || obj === null)
            return null;
        switch (typeof obj) {
            case "string":
            case "number":
                return new MPIDJson(obj);
            default:
                var id = obj.id;
                if (id !== undefined)
                    return new MPIDJson(id);
                break;
        }
        return null;
    }
}

class MPTag {

    static AmplitudeUnits = "mont/pd/au";
    static SensorType = "mont/pd/mt";
    static AverageAmplitude = "mont/pd/magAv";
    static PdDataset = "bin:mont/pd";
    static PdDatasetFeatures = "bin:mont/pd/wf/features";
    static PdDatasetWaveforms = "bin:mont/pd/wf/raw";
    static PdClassEnum = "sys:mont/pd/dia/class/enum";
    static ConnectionState = "sys:cs";
    static TestpointState = "sys:st";
    
    constructor(tag) {
        this.key = String(tag.key);
        this.title = tag.title !== undefined ? String(tag.title) : null;
        this.units = tag.units !== undefined ? String(tag.units) : null;
        this.parent = tag.parent !== undefined ? String(tag.parent) : null;
        this.saveType = tag.saveType !== undefined ? Number(tag.saveType) : 0;
        this.state = tag.state !== undefined ? Boolean(tag.state) : false;
        this.trend = tag.trend !== undefined ? Boolean(tag.trend) : false;
        this.bit = tag.bit !== undefined ? Boolean(tag.bit) : false;
        this.hidden = tag.hidden !== undefined ? Boolean(tag.hidden) : false;
        this.prop = tag.prop !== undefined ? Number(tag.prop) : false;
        this.unitLink = tag.unitLink !== undefined ? String(tag.unitLink) : null;
        this.link = tag.link !== undefined ? String(tag.link) : null;
    }

    static getUnitTag(tagMap, valueTag) {

        var tag = tagMap.get(valueTag);
        if (tag == null) return null;

        if (tag?.unitLink == null || tag.unitLink === "")
            return null;
        var unitTag = tagMap.get(tag.unitLink);
        if (unitTag == null)
            return null;
        return unitTag;
    }

    getBit(bitNumber) {
        if ((this.prop & (1 << bitNumber)) != 0)
            return true;
        return false;
    }

    isDataset() {
        if (this.getBit(0))
            return true;
        return false;
    }

    isDatasetPrimary() {
        if (getBit(1))
            return true;
        return false;
    }

    canPreview() {
        switch (this.key) {
            case MPTag.PdDatasetFeatures:
            case MPTag.PdDatasetWaveforms:
                return false;
            case MPTag.SensorType:
            case MPTag.AmplitudeUnits:
                return true;
        }

        if (this.hidden || this.state)
            return false;
        if (this.getBit(4))
            return false;
        return true;
    }
}

class MPValue {

    static Binary = 0;
    static Float = 1;
    static Int32 = 2;
    static UTF8String = 3;
    static Boolean = 4;
    static Byte = 5;
    static SByte = 6;
    static Int64 = 7;
    static Password = 8;
    static ValueStream = 9;

    constructor(testPointId, value) {
        this.id = value.id != null ? String(value.id) : null;
        this.key = value.key != null ? String(value.key) : null;
        this.dt = value.dt != null ? new Date(value.dt) : null;
        this.type = value.type != null ? Number(value.type) : 0;
        this.val = value.val != null ? String(value.val) : null;
        this.dt = value.dt != null ? new Date(value.dt) : null;
        this.tag = null;
        this.testPointId = new MPIDJson(testPointId);
    }

    isBoolean() {
        switch (this.type) {
            case MPValue.Boolean:
                return true;
        }
        return false;
    }

    isNumber() {
        switch (this.type) {
            case MPValue.Float:
            case MPValue.Int32:
            case MPValue.Boolean:
            case MPValue.Byte:
            case MPValue.SByte:
            case MPValue.Int64:
                return true;
        }
        return false;
    }

    toNumber() {
        if (this.val === null || this.val == "")
            return NaN;
        switch (this.type) {
            case MPValue.Float:
            case MPValue.Int32:
            case MPValue.Boolean:
            case MPValue.Byte:
            case MPValue.SByte:
            case MPValue.Int64:
                return Number(this.val);
        }
        return NaN;
    }

    toObject() {
        switch (this.type) {
            case MPValue.Boolean:
                return Boolean(Number(this.val));
            case MPValue.Float:
            case MPValue.Int32:
            case MPValue.Byte:
            case MPValue.SByte:
            case MPValue.Int64:
                return Number(this.val);
            case MPValue.Binary:
                return MPBinaryConverter.base64ToUint8Array(this.val);
            case MPValue.Password:
            case MPValue.UTF8String:
                return this.val;
        }
        return null;
    }

    toString() {
        return this.tag.title + ": " + toNumber();
    }
}

class MPSettings {
    constructor(value) {
        this.params = [];
        if (value?.params != null) {
            for (const v of value.params)
                this.params.push(new MPValue(null, v));
        }
    }
}

class MPIDMultipleJson {
    constructor() {
        this.items = [];
    }

    add(obj) {
        const id = MPIDJson.create(obj)
        if (id !== null)
            this.items.push(id);
    }

    addRange(items) {
        for (const i of items) {
            this.add(i);
        }
    }

    static create(testpointIdList) {
        var result = new MPIDMultipleJson();
        if (testpointIdList !== null) {
            result.addRange(testpointIdList);
        }
        return result;
    }

}

class MPIndexRequest {
    constructor(uniqueId, id, from, to, tag = null) {
        this.uniqueId = String(uniqueId);
        this.id = MPIDJson.create(id);
        this.from = from;
        this.to = to;
        this.tag = tag;
    }
}

class MPLocationCompareRequestData {
    constructor(key) {
        this.key = String(key);
        this.filter = null;
    }

    filterAdd(point) {
        if (point?.x !== undefined && point?.y !== undefined) {
            if (this.filter === null)
                this.filter = [];
            this.filter.push(new MPPoint(x, y));
        }
    }
}

class MPLocationCompareRequest {
    constructor(aKey, bKey) {
        this.times = [];
        this.absoluteDistance = null;
        this.relativeMax = 100;
        this.vop = null;
        this.a = new MPLocationCompareRequestData(aKey);
        this.b = new MPLocationCompareRequestData(bKey);
    }

    static createFromKeys(a, b) {
        return new MPLocationCompareRequest(a, b);
    }

    static createFromTestPoints(a, b) {
        return new MPLocationCompareRequest(a.key, b.key);
    }

    withDate(date) {
        if (date !== undefined && date !== null)
            this.times.push(date);
        return this;
    }

    withDates(dates) {
        if (dates !== undefined && dates !== null) {
            for (const i of dates)
                this.times.push(i);
        }
        return this;
    }

    withAFilter(points) {
        if (points !== undefined && points !== null) {
            for (const i of points)
                this.a.filterAdd(i);
        }
        return this;
    }

    withBFilter(points) {
        if (points !== undefined && points !== null) {
            for (const i of points)
                this.b.filterAdd(i);
        }
        return this;
    }

    withVop(vop) {
        if (vop !== undefined && vop !== null) {
            this.vop = Number(vop);
            if (this.vop < 0) this.vop = 0;
            if (this.vop > 100) this.vop = 100;
        }
        return this;
    }

    withAbsoluteDistance(value) {
        if (value !== undefined && value !== null) {
            this.absoluteDistance = Number(value);
            this.relativeMax = null;
        }
        return this;
    }

    withRelativeDistance(value) {
        if (value !== undefined && value !== null) {
            this.absoluteDistance = null;
            this.relativeMax = Number(value);
        }
        return this;
    }
}

class MPLocationTestPoint {
    constructor(value) {
        this.key = value.key === undefined ? null : String(value.key);
        this.name = value.name === undefined ? null : String(value.name);
        this.visible = [];
        this.hidden = [];
        if (value.visible !== undefined) {
            for (const i of value.visible) {
                this.visible.push(new MPPoint(i.x, i.y));
            }
        }
        if (value.hidden !== undefined) {
            for (const i of value.hidden) {
                this.hidden.push(new MPPoint(i.x, i.y));
            }
        }
    }
}

class MPLocationResult {
    constructor(value) {
        this.success = value.success === undefined ? false : Boolean(value.success);
        this.isRelative = value.isRelative === undefined ? false : Boolean(value.isRelative);
        this.result = value.result === undefined ? 0 : Number(value.result);
        this.pairCount = value.pairCount === undefined ? 0 : Number(value.pairCount);
        this.a = value.a === undefined ? null : new MPLocationTestPoint(value.a);
        this.b = value.b === undefined ? null : new MPLocationTestPoint(value.b);
        this.countDistribution = value.countDistribution === undefined ? 0 : value.countDistribution;
    }
}

class MPStatRequest extends MPIDMultipleJson {
    constructor(includeDataSourceInfo = false) {
        super();
        this.ids = Boolean(includeDataSourceInfo);
    }
    static create(idList, includeDataSourceInfo) {
        var result = new MPStatRequest();
        result.includeDataSourceInfo = Boolean(includeDataSourceInfo);
        if (idList !== undefined && idList !== null) {
            for (const t of idList) {

                result.add(t);
            }
        }
        return result;
    }

}

class MPStatItem {
    static UnknownState = -1;
    static OkState = 0;
    static WarningState = 1;
    static AlarmState = 2;

    static ConnectionOk = 10;
    static ConnectionFailure = 11;
    static TestpointCount = 12;
    static ServerDatasetCount = 13;
    static HasNewData = 15;
    static ConnectionUnknown = 16;
    constructor(value, parent) {
        this.v = value.v === undefined ? 0 : Number(value.v);
        this.t = value.t === undefined ? 0 : Number(value.t);
        this.parent = parent;
    }
}

class MPStateInfo extends MPIDJson {
    constructor(id) {
        super(id);
        this.countTestPoint = 0;

        this.countStateOk = 0;
        this.countStateWarning = 0;
        this.countStateAlarm = 0;
        this.countStateUndefined = 0;

        this.countConnected = 0;
        this.countDisconnected = 0;
        this.countDisabled = 0;

        this.maxDatasetTime = null;
        this.maxUpdateTime = null;
    }
}

class MPStat extends MPIDJson {
    constructor(value, parent) {
        super(value.id);
        this.parent = parent;
        this.items = [];
        if (value.items !== undefined) {
            for (const i of value.items) {
                this.items.push(new MPStatItem(i, this));
            }
        }
        this.time = value.time === undefined ? null : new Date(value.time);
        this.timeUpdate = value.timeUpdate === undefined ? null : new Date(value.timeUpdate);
    }
}

class MPStatList {
    constructor(value) {
        this.eq = [];
        this.serverStart = value.serverStart === undefined ? null : new Date(value.serverStart);
        if (value.eq !== undefined) {
            for (const i of value.eq) {
                this.eq.push(new MPStat(i, this));
            }
        }


    }
    * getStateInfo() {

        for (const e of this.eq) {
            var info = new MPStateInfo(e.id);
            if (info.maxDatasetTime === null)
                info.maxDatasetTime = e.time;
            else if (e.time !== null && e.time > info.maxDatasetTime)
                info.maxDatasetTime = e.time;

            if (info.maxUpdateTime === null)
                info.maxUpdateTime = e.timeUpdate;
            else if (e.timeUpdate !== null && e.timeUpdate > info.maxDamaxUpdateTimetasetTime)
                info.maxUpdateTime = e.timeUpdate;

            for (const i of e.items) {
                switch (i.t) {
                    case MPStatItem.TestpointCount:
                        info.countTestPoint = i.v;
                        break;
                    case MPStatItem.UnknownState:
                        info.countStateUndefined += i.v;
                        break;
                    case MPStatItem.OkState:
                        info.countStateOk += i.v;
                        break;
                    case MPStatItem.WarningState:
                        info.countStateWarning += i.v;
                        break;
                    case MPStatItem.AlarmState:
                        info.countStateAlarm += i.v;
                        break;
                    case MPStatItem.ConnectionOk:
                        info.countConnected += i.v;
                        break;
                    case MPStatItem.ConnectionFailure:
                        info.countDisconnected += i.v;
                        break;
                    case MPStatItem.ConnectionUnknown:
                        info.countDisabled += i.v;
                        break;
                }
            }
            yield info;
        }
    }
}

class MPDataRequest {
    constructor() {
        this.testpoints = [];
        this.ignore = [];
        this.include = [];
        this.noValue = false;
        this.timestamp = null;
    }
}

class MPTimeRequest extends MPIDJson {
    constructor(id, from, to) {
        super(id);
        this.from = new Date(from);
        this.to = new Date(to);
    }
}

class MPTimeRequestSelected extends MPTimeRequest {
    constructor(id, from, to, time = null) {
        super(id, from, to);
        this.time = time;
    }
}

class MPSingleRequest extends MPTimeRequestSelected {
    constructor(id, from, to, time = null, left = undefined, right = undefined, tag = undefined, ignore = undefined, postProcess = undefined) {
        super(id, from, to, time);
        this.left = left === undefined ? false : Boolean(left);
        this.right = right === undefined ? false : Boolean(right);
        this.tag = tag === undefined ? null : String(tag);
        this.ignore = ignore === undefined ? null : [...ignore];
        this.postProcess = postProcess === undefined ? false : Boolean(postProcess);
    }
}

class MPArchiveRequest extends MPTimeRequest {
    constructor(id, from, to, type = 0) {
        super(id, from, to);
        this.type = Number(type);
    }
}

class MPEventRequest extends MPArchiveRequest {
    constructor(id, from, to, withActive = false, withConnectionState = false) {
        super(id, from, to);
        this.withActive = withActive;
        this.withConnectionState = withConnectionState;
        this.testpoints = new MPIDMultipleJson();
    }
}

class MPStreamRequest {
    constructor() {
        this.items = [];
        this.enable = false;
        this.rec = false;
        this.session = null;
        this.download = false;
        this.datasetTime = null;
    }
}

class MPItem extends MPIDJson {

    static TypeEquipment = 0;
    static TypeTestPoint = 1;
    static TypeDataSource = 2;
    static TypeFile = 3;

    constructor(id, name, type, parent) {
        super(id);
        this.parent = parent;
        this.name = name !== undefined ? name : "";
        this.itemType = type;
    }
    async * enumerateRecursiveAsync(client) {
        yield this;
    }

    * getChildrenAsync(client) {
        yield;
    }

    isTestPoint() {
        return this.itemType === MPItem.TypeTestPoint;
    }

    isDataSource() {
        return this.itemType === MPItem.TypeDataSource;
    }

    isEquipment() {
        return this.itemType === MPItem.TypeEquipment;
    }

    isFile() {
        return this.itemType === MPItem.TypeFile;
    }


    static defaultSort(a, b) {
        if (a.name < b.name) return -1;
        if (a.name > b.name) return 1;
        return 0;
    }

    sort(a, b) {
        return MPItem.defaultSort(a, b);
    }
}

class MPTreeItem extends MPItem {
    constructor(id, name, type, parent) {
        super(id, name, type, parent);
        this.children = null;
    }
}

class MPTestPointConstant {
    constructor(value, parent) {
        this.parent = parent;
        this.plugin = value.plugin === undefined ? null : String(value.plugin);
        this.tespointId = value.tespointId !== undefined ? new MPIDJson(value.tespointId.id) : null;
        this.tespointDataId = value.tespointDataId !== undefined ? new MPIDJson(value.tespointDataId.id) : null;
        this.value = null;
        this.default = null;
        this.units = null;
        this.threshold = value.threshold === undefined ? 0 : Number(value.threshold);
        this.plugins = value.plugins === undefined ? [] : value.plugins;
        this.states = value.states === undefined ? [] : value.states;
    }

    getActiveValue() {
        if (this.value !== null)
            return this.value;
        if (this.default !== null)
            return this.default;
        return null;
    }

    getValueOrDefault() {
        const value = this.getActiveValue();
        if (value !== null)
            return value.toObject();
        return NaN;
    }

    isNumberOrBoolean() {
        const value = this.getActiveValue();
        return (value !== null) && (value.isNumber() || value.isBoolean());
    }

    getUnits() {
        if (this.units != null && this.units.isNumber())
            return MPAmplitudeUnit.getText(this.units.toNumber());
        return null;
    }

    getTitle() {
        const value = this.getActiveValue();
        if (value.tag !== null) {
            return this.default.tag.title;
        }
        return null;
    }

    getFullTitle() {
        const title = this.getTitle()
        const units = this.getUnits()
        if (units === null) return title;
        return title + ", " + units;
    }

    isState() {
        return this.threshold >= 0;
    }

    getFullTitleWithState() {

        const title = this.getFullTitle()
        if (this.isState()) {
            var state = MPState.getText(this.threshold);
            return title + " (" + state + ")";
        }
        return title;
    }

    toString() {
        return this.getFullTitleWithState() + ": " + this.getValueOrDefault();
    }
}

class MPTestPoint extends MPItem {
    constructor(value, parent) {
        super(value.id, value.name, MPItem.TypeTestPoint, parent);
        this.key = value.key == null ? null : String(value.key);
        this.title = (value.name !== undefined && value.name !== null && value.name !== "") ? value.name : value.key;
        this.idEq = value.idEq == null ? null : String(value.idEq);
        this.idDs = value.idDs == null ? null : String(value.idDs);
        this.enabled = value.enabled == null ? null : Boolean(value.enabled);
        this.settings = value.settings == null ? null : new MPSettings(value.settings);
    }

    sort(a, b) {
        if (a.title < b.title) return 1;
        if (a.title > b.title) return -1;
        return 0;
    }

    #getValue(source, client) {
        if (source !== undefined) {
            var result = new MPValue(this.id, source);
            var tag = client.tags.get(source.key);
            if (tag !== undefined)
                result.tag = tag;

            return result;
        }
        return null;
    }

    async * getConstantsAsync(client) {
        var response = await client.postJsonAndCheckAsync("/api/testpointConstants", client.request(new MPIDJson(this.id)));
        for (const c of response.data) {
            var constant = new MPTestPointConstant(c, this);
            constant.value = this.#getValue(c.value, client);
            constant.units = this.#getValue(c.units, client);
            constant.default = this.#getValue(c.default, client);
            yield constant;
        }
    }
}

class MPFile extends MPItem {
    ExtensionSvg = "svg"
    ExtensionGlblm = "glblm";

    constructor(value, parent) {
        super(value.id, value.name, MPItem.TypeFile, parent);
        this.title = value.title === undefined ? null : String(value.title);
        this.data = null;
    }

    getExtension() {
        const parts = this.name.split('.');
        var result = parts.pop();
        if (result != null)
            return result.toLowerCase();
        return null;
    }

    async getDataAsync(client) {
        var fileResponse = await client.postJsonAndCheckAsync("/api/file", client.request(new MPIDRequestJson(this.id, false, true)));
        for (const f of fileResponse.data) {
            if (f.data !== undefined) {
                return MPBinaryConverter.base64ToUint8Array(f.data);
            }
            break;
        }
        return null;
    }


    static defaultSort(a, b) {
        if (a.title < b.title) return 1;
        if (a.title > b.title) return -1;
        return 0;
    }

    sort(a, b) {
        return MPFile.defaultSort(a, b);
    }
}

class MPEquipment extends MPTreeItem {
    #isCHildrenLoaded = false;
    constructor(id, name, type, parent) {
        super(id, name, MPItem.TypeEquipment, parent);
        this.type = type === undefined ? 0 : Number(type);
        this.children = [];
        this.#isCHildrenLoaded = false;
    }

    async #loadChildrenAsync(client) {
        this.children = [];

        let [responseEquipment, responseTestPoint, responseFiles] = await Promise.all([
            client.postJsonAndCheckAsync("/api/equipment", client.request(new MPIDRequestJson(this.id, true))),
            client.postJsonAndCheckAsync("/api/testpoint", client.request(new MPIdRequestChildrenData(this.id))),
            client.postJsonAndCheckAsync("/api/file", client.request(new MPIDRequestJson(this.id, true)))

        ]);

        if (responseFiles !== null) {

            responseFiles.data.sort(MPItem.defaultSort);
            for (const f of responseFiles.data) {
                this.children.push(new MPFile(f, this));
            }
        }

        if (responseEquipment !== null) {
            responseEquipment.data.sort(MPFile.defaultSort);
            for (const eq of responseEquipment.data) {
                if (eq.idParent !== undefined && Number(this.id) === eq.idParent)
                    this.children.push(new MPEquipment(eq.id, eq.name, eq.type, this));
            }
        }

        if (responseTestPoint !== null) {
            responseTestPoint.data.sort(MPItem.defaultSort);
            for (const t of responseTestPoint.data) {
                this.children.push(new MPTestPoint(t, this));
            }
        }

        this.#isCHildrenLoaded = true;
    }

    refresh() {
        this.children = [];
        this.#isCHildrenLoaded = false;
    }

    async * getChildrenAsync(client) {
        if (!this.#isCHildrenLoaded) {
            await this.#loadChildrenAsync(client);
        }
        for (const c of this.children) {
            yield c;
        }
    }

    async * enumerateRecursiveAsync(client) {
        yield this;
        for await (const c of this.getChildrenAsync(client)) {
            for await (const cc of c.enumerateRecursiveAsync(client)) {
                yield cc;
            }

        }
    }

}

class MPDataSource extends MPItem {
    constructor(ds) {
        super(ds.id, ds.name, MPItem.TypeDataSource);
        this.plugin = ds.plugin === undefined ? null : String(ds.plugin);
        this.enabled = ds.enabled === undefined ? null : Boolean(ds.enabled);
        this.settings = ds.settings == null ? null : new MPSettings(ds.settings);
    }
}

class MPIDRequestJson extends MPIDJson {
    constructor(id, needChildren = true, needAllData = false, fullscope = false) {
        super(id);
        this.needChildren = Boolean(needChildren);
        this.needAllData = Boolean(needAllData);
        this.fullscope = Boolean(fullscope);

    }
}

class MPIDChildrenJson extends MPIDRequestJson {
    constructor(id, needChildren = true, needAllData = false, fullscope = false) {
        super(id, needChildren, needAllData, fullscope);
        this.children = [];
    }
}

class MPIdRequestChildrenData extends MPIDRequestJson {
    constructor(id, needChildren = true, needAllData = false, fullscope = false) {
        super(id, needChildren, needAllData, fullscope);
        this.children = [];
    }

    addChild(childId) {
        this.addChild.push(MPIDJson.create(childId));
    }
}

class MPRequestCuture extends MPRequest {
    constructor(token, culture, data = null) {
        super(token, data);
        this.culture = String(culture);
    }
}

class MPPdeNativeName {
    static corona = "1 - corona";
    static floating = "2 - floating potential";
    static surface = "3 - surface discharge";
    static internal = "4 - internal discharge";
    static particle = "5 - particle discharge";
    static external = "6 - external disturbance";
    static repetitive = "7 - repetitive discharge";
    static floatingNarrow = "8 - floating narrow";
    static vibration = "9 - vibration";

    constructor(value) {
        this.nativeName = value.nativeName === undefined ? null : String(value.nativeName);
    }

    static zdsClassIndex(nativeName) {
        if (nativeName == null)
            return 0;
        switch (nativeName) {
            case MPPdeNativeName.corona:
                return 1;
            case MPPdeNativeName.floating:
                return 2;
            case MPPdeNativeName.surface:
                return 3;
            case MPPdeNativeName.internal:
                return 4;
            case MPPdeNativeName.particle:
                return 5;
            case MPPdeNativeName.external:
                return 6;
            case MPPdeNativeName.repetitive:
                return 7;
            case MPPdeNativeName.floatingNarrow:
                return 8;
            case MPPdeNativeName.vibration:
                return 9;
            default:
                return 0;
        }
    }

    getIndex() {
        return MPPdeNativeName.zdsClassIndex(this.nativeName);
    }

    toString() {
        return this.nativeName;
    }
}

class MPPdeNativeNameAndInfo extends MPPdeNativeName {
    constructor(value) {
        super(value);
        this.name = value.name === undefined ? null : String(value.name);
        this.desc = value.desc === undefined ? null : String(value.desc);
    }

    toString() {
        return this.name;
    }
}

class MPPdeClassInfo extends MPPdeNativeNameAndInfo {
    constructor(value) {
        super(value);
        this.classId = value.classId !== undefined ? Number(value.classId) : null;
        this.className = value.className !== undefined ? String(value.className) : null;
        this.level = value.level !== undefined ? Number(value.level) : null;
        this.commonName = value.commonName !== undefined ? Number(String.commonName) : null;
    }

    toString() {
        return this.name;
    }
}

class MPPdeResultClass extends MPPdeNativeNameAndInfo {
    constructor(value) {
        super(value);
        this.service = value.service !== undefined ? Number(value.service) : 0;
        this.value = value.value !== undefined ? Number(value.value) : 0;
        this.state = value.state !== undefined ? Number(value.state) : 0;
        this.className = value.className !== undefined ? String(value.className) : null;
        this.classId = value.classId !== undefined ? Number(value.classId) : null;
    }

    toString() {
        return this.name + ", " + Math.round(this.value * 100) + "%";
    }
}

class MPPdeResponseClass extends MPPdeNativeName {
    constructor(value) {
        super(value);
        this.value = value.value !== undefined ? Number(value.value) : 0;
        this.className = value.className !== undefined ? String(value.className) : null;
        this.classId = value.classId !== undefined ? Number(value.classId) : null;
    }

    toString() {
        return this.name + ", " + Math.round(this.value * 100) + "%";
    }
}

class MPPdeResponse extends MPPdeResultClass {
    constructor(value) {
        super(value);
        this.stateTestpoint = value.stateTestpoint !== undefined ? Number(value.stateTestpoint) : 0;
        if (value.probability === undefined)
            this.probability = [];
        else {
            this.probability = [];
            for (const i of value.probability) {
                this.probability.push(new MPPdeResponseClass(i));
            }
        }
        if (value.other === undefined)
            this.other = [];
        else {
            this.other = [];

            for (const i of value.other) {
                this.other.push(new MPPdeResultClass(i));
            }
        }
    }
}

class MPRectArea {
    constructor(minX, maxX, minY, maxY) {
        this.minX = minX === undefined ? 0 : Number(minX);
        this.maxX = maxX === undefined ? 0 : Number(maxX);
        this.minY = minY === undefined ? 0 : Number(minY);
        this.maxY = maxY === undefined ? 0 : Number(maxY);
    }
}

class MPPdeRequest {
    constructor(id, tag, time, culture, getState = false, minX = undefined, maxX = undefined, minY = undefined, maxY = undefined) {
        this.id = MPIDJson.create(id);
        this.tag = tag;
        this.time = time;
        this.culture = culture === undefined ? null : String(culture);
        this.getState = getState;
        if (minX === undefined || maxX === undefined || minY === undefined || maxY === undefined)
            this.tfFilter = null;
        else
            this.tfFilter = new MPRectArea(minX, maxX, minY, maxY);
    }
}

class MPAccumulationResult {
    constructor(value, testPointId) {
        if (value === undefined) {
            this.pde = null;
            this.value = null;
        }
        else {
            this.pde = value.pde === undefined ? null : new MPPdeResponse(value.pde);
            this.value = value.value === undefined ? null : new MPValue(testPointId, value.value);
        }
    }
}

class MPEvent {
    constructor(parent, state, start, idEq, idtp) {
        this.parent = parent;
        this.state = state;
        this.start = start;
        this.end = null;
        this.idEq = idEq;
        this.idtp = idtp;
        this.satelliteValue = null;
    }

    getTestPointName() {
        return this.parent.parent.namesTp.get(this.idtp);
    }

    getEquipmentName() {
        return this.parent.parent.namesEq.get(this.idEq);
    }

    getSatelliteText() {
        if (this.satelliteValue === null || this.parent === null || this.parent.satelliteTag === null)
            return null;
        switch (this.parent.satelliteTag.key) {
            case MPTag.PdClassEnum:
                var pdClass = this.parent.parent.pdClasses.get(this.satelliteValue);
                if (pdClass !== undefined)
                    return pdClass.name;
                break;
            case MPTag.AverageAmplitude:
                {
                    const displaySettings = this.parent.displaySettings.get(this.idtp);
                    return displaySettings.getValueText(this.satelliteValue);
                }
                break;
        }
        return null;
    }

    toString() {
        return this.getEquipmentName() + "/" + this.getTestPointName() + "/" + this.state + "/" + this.getSatelliteText();
    }
}

class MPEventGroup {
    constructor(tag, parent) {
        this.parent = parent;
        this.tag = tag;
        this.events = [];
        this.satelliteTag = null;
        this.displaySettings = new Map();
    }
}

class MPEventList {
    constructor() {
        this.groups = new Map();
        this.pdClasses = new Map();
        this.namesEq = new Map();
        this.namesTp = new Map();
    }
}

class MPStreamSubscription {
    constructor(testPoint) {
        this.testPoint = testPoint;
        this.timestamp = new Date();
        this.callback = null;
    }

    onDataReceived(frames) {
        if (this.callback !== null)
            this.callback(frames);
    }
}

class MPPluginInformation {
    constructor(value) {
        this.key = value.key === undefined ? null : String(value.key);
        this.cfg = value.options.cfg === undefined ? null : Boolean(value.options.cfg);
        this.cfgShare = value.options.cfgShare === undefined ? null : Boolean(value.options.cfgShare);
        this.proxy = value.options.proxy === undefined ? null : Boolean(value.options.proxy);
        this.nav = value.options.nav === undefined ? null : Boolean(value.options.nav);
        this.prps = value.options.prps === undefined ? null : Boolean(value.options.prps);
        this.search = value.options.search === undefined ? null : Boolean(value.options.search);
    }
}

class MPRequestLogin extends MPRequest {
    constructor(token, user, password, data = null) {
        super(token, data);
        this.user = user == null || user === "" ? null : String(user);
        this.password = password == null || password === "" ? null : String(password);
        this.scheme = null;
    }
}

class MPUser {
    static ClaimName = "name";
    static ClaimRole = "role";
    static ClaimJwt = "jwt";
    static Admin = 0;
    constructor() {
        this.token = null;
        this.claims = new Map();
    }

    reset() {
        this.token = null;
        this.claims = new Map();
    }

    setClaim(key, value) {
        this.claims.set(String(key), String(value));
    }

    getClaim(key) {
        var result = this.claims.get(String(key));
        return result == null ? null : result;
    }

    getName() {
        return getClaim(MPUser.ClaimName);
    }

    getRole() {
        return Number(getClaim(MPUser.ClaimRole));
    }

    isAdmin() {
        return getRole() === MPUser.Admin;
    }

    getJwt() {
        return getClaim(MPUser.ClaimJwt);
    }

    toString() {
        return getName();
    }
}

class MPLoginIformation {
    static TokenSchemeGlb = "GlbToken";
    static TokenSchemeGlbJwt = "GlbTokenJwt";
    static TokenStorageKey = "sd400-mp-auth.token";
    constructor() {
        this.currentUser = new MPUser();
        this.user = null;
        this.culture = null;
        this.password = null;
        this.apiVersion = -1;
        this.authorizationHeaderCallback = null;
    }

    update(response) {
        this.currentUser.token = response.token == null ? null : response.token;
        if (response.params != null) {
            for (const p of response.params) {
                this.currentUser.setClaim(p.key, p.value);
            }
        }
    }

    #getAuthorizationHeader() {
        if (this.authorizationHeaderCallback != null)
            return String(this.authorizationHeaderCallback());
        return MPLoginIformation.TokenSchemeGlb + " " + this.currentUser.token;
    }

    getDefaultGetHeaders() {
        return {
            "Authorization": this.#getAuthorizationHeader()
        };
    }

    getDefaultPostHeaders() {
        return {
            "Authorization": this.#getAuthorizationHeader(),
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        };
    }

    createLoginRequest() {
        var result = new MPRequestLogin(null, this.user, this.password);
        var jwt = this.currentUser.getClaim(MPUser.ClaimJwt);
        if (jwt != null) {
            result.scheme = MPLoginIformation.TokenSchemeGlbJwt;
            result.token = jwt;
        }
        return result;
    }
}

class MPClient {
    constructor(baseAddress) {
        this.baseAddress = String(baseAddress).trim();
        this.loginInformation = new MPLoginIformation();
        this.tags = new Map();
        this.equipment = [];
        this.subscriptions = new Map();
        this.plugins = new Map();
    }

    #storageSave(key, value) {
        try {
            if (typeof (Storage) != "undefined") {
                localStorage.setItem(key, JSON.stringify(value));
            }
        }
        catch (e) {
            console.log(e);
        }
    }

    #storageLoad(key) {
        try {
            if (typeof (Storage) !== "undefined") {
                var value = localStorage.getItem(key);
                if (value !== undefined && value !== null) {
                    var result = JSON.parse(value);
                    return result;
                }
            }
            return null;
        }
        catch (e) {
            console.log(e);
            return null;
        }
    }

    runPromise(promise) {
        return promise.then(data => [null, data])
            .catch(err => [err]);
    }

    async runPromiseData(promise) {
        const [err, data] = await this.runPromise(promise);
        return data;
    }

    #checkResponse(response) {
        if (response !== null && response.code !== undefined && response.code !== 200) {
            if (response.error !== undefined)
                throw new Error(response.error + " (Code: " + response.code + ")");
            throw new Error("Response error code: " + response.code);
        }
    }

    async fetchJsonAsync(address) {
        try {
            address = this.baseAddress + address;
            const [err, data] = await this.runPromise(fetch(address, {
                headers: this.loginInformation.getDefaultGetHeaders()
            }));
            if (err) return null;
            const json = data.json();
            return json;
        }
        catch (e) {
            console.log(e);
            return null;
        }
    }

    async postJsonAsync(address, request) {
        try {

            address = this.baseAddress + address;
            const [err, data] = await this.runPromise(fetch(address, {
                method: 'POST',
                headers: this.loginInformation.getDefaultPostHeaders(),
                body: JSON.stringify(request)
            }));
            if (err) return null;

            const json = data.json();

            return json;
        }
        catch (e) {
            console.log(e);
            return null;
        }
    }

    async postJsonAndCheckAsync(address, request) {
        var response = await this.postJsonAsync(address, request);
        this.#checkResponse(response);
        return response;
    }

    request(data = null) {
        return new MPRequest(this.loginInformation.currentUser.token, data);
    }

    requestCulture(data = null) {
        return new MPRequestCuture(this.loginInformation.currentUser.token, this.loginInformation.culture, data);
    }

    async loginAsync() {

        var savedJwt = this.#storageLoad(MPLoginIformation.TokenStorageKey);
        if (savedJwt != null && savedJwt != "")
            this.loginInformation.currentUser.setClaim(MPUser.ClaimJwt, savedJwt);

        const response = await this.postJsonAsync("/api/auth", this.loginInformation.createLoginRequest());
        this.loginInformation.currentUser.reset();
        if (response !== null && response.code === 200) {
            this.loginInformation.update(response);
            var jwt = this.loginInformation.currentUser.getClaim(MPUser.ClaimJwt);
            if (jwt != null)
                this.#storageSave(MPLoginIformation.TokenStorageKey, jwt);
            let [versionResponse, tagsResponse, rootLevelResponse, pluginResponse] = await Promise.all([
                this.fetchJsonAsync("/api/version"),
                this.postJsonAsync("/api/tagsJson", this.requestCulture()),
                this.postJsonAndCheckAsync("/api/equipment", this.request({ needChildren: true })),
                this.postJsonAndCheckAsync("/api/plugins", this.request({ needChildren: true }))
            ]);
            this.plugins = new Map();
            for (const p of pluginResponse.data.datasources) {
                this.plugins.set(p.key, new MPPluginInformation(p));
            }
            this.loginInformation.apiVersion = versionResponse.api;

            this.tags = new Map();

            tagsResponse.data.items.forEach(t => {
                const tag = new MPTag(t);
                this.tags.set(tag.key, tag)
            });

            this.equipment = [];
            rootLevelResponse.data.forEach(eq => {
                this.equipment.push(new MPEquipment(eq.id, eq.name, eq.type, null));
            });
        }
        else
            throw new Error("Login error");
    }

    async * enumerateRecursiveAsync() {
        for (const e of this.equipment) {
            for await (const c of e.enumerateRecursiveAsync(this)) {
                yield c;
            }
        }
    }

    * getPreviewTags() {

        for (const t of this.tags) {
            if (t[1].canPreview())
                yield t[1].key;
        }
    }

    async * #getMultipleTestPointDataAsync(request) {
        var data = await this.postJsonAndCheckAsync("/api/data", this.request(request));
        if (data !== null && data.code === 200 && data.data !== undefined && data.data.groups !== undefined) {
            for (const g of data.data.groups) {

                for (const v of g.online) {
                    var value = new MPValue(g.id, v);
                    value.tag = this.tags.get(v.key);
                    yield value;
                }
            }
        }
    }

    async * getMultipleTestPointDataAsync(testpointIdList, include = null, ignore = null, noValue = false, timestamp = null) {
        var request = new MPDataRequest();
        request.noValue = noValue;
        request.timestamp = timestamp;
        for (let id of testpointIdList) {
            request.testpoints.push(MPIDJson.create(id))
        }

        if (ignore != null) {
            for (let i of ignore) {
                request.ignore.push(i)
            }
        }

        if (include != null) {
            for (let i of include) {
                request.include.push(i)
            }
        }

        for await (const t of this.#getMultipleTestPointDataAsync(request)) {
            yield t;
        }
    }

    async * getSingleTestPointDataAsync(testpointId, include = null, ignore = null, noValue = false, timestamp = null) {
        for await (const t of this.getMultipleTestPointDataAsync([testpointId], include, ignore, noValue, timestamp)) {
            yield t;
        }
    }

    async * getArchiveAsync(dataId, from, to, type) {
        var request = new MPArchiveRequest(dataId, from, to, type)
        var data = await this.postJsonAndCheckAsync("/api/archive", this.request(request));
        if (data !== null && data.code === 200 && data.data.payload !== undefined && data.data.payload !== "") {
            for (const p of MPBinaryConverter.dataPointsFromBase64(data.data.payload)) {
                
                yield p;
            }
        }
    }

    async getEventListAsync(idEquipment, from, to, testPointIdArray = null, withConnectionState = false) {
        var request = new MPEventRequest(idEquipment, from, to, false, withConnectionState);

        if (testPointIdArray !== null) {
            request.testpoints = MPIDMultipleJson.create(testPointIdArray);
        }
        var data = await this.postJsonAndCheckAsync("/api/events", this.request(request));
        if (data !== null && data.code === 200) {
            var result = new MPEventList();
            var classes = await this.postJsonAndCheckAsync("/api/pdeClasses", this.requestCulture());
            if (classes !== null) {

                for (const c of classes.data) {
                    var pdc = new MPPdeClassInfo(c);
                    result.pdClasses.set(pdc.getIndex(), pdc);
                }
            }

            var eqList = [];
            var tpList = [];

            for (const eq of data.data.equipment) {

                eqList.push(eq.id);
                for (const tp of eq.testpoints) {
                    tpList.push(tp.id);
                }
            }

            if (eqList.length > 0 && tpList.length > 0) {

                let [namesTp, namesEq] = await Promise.all([
                    this.postJsonAsync("/api/namestp", this.request(MPIDMultipleJson.create(tpList))),
                    this.postJsonAndCheckAsync("/api/nameseq", this.request(MPIDMultipleJson.create(eqList)))
                ]);
                for (const i of namesEq.data)
                    result.namesEq.set(i.id, i.name);
                for (const i of namesTp.data)
                    result.namesTp.set(i.id, i.name);
            }

            for (const eq of data.data.equipment) {

                for (const tp of eq.testpoints) {
                    var current = null;
                    for (const t of tp.tags) {

                        var tag = this.tags.get(t.tag);
                        if (tag === undefined) continue;

                        var group = result.groups.get(tag.key)
                        if (group === undefined) {
                            group = new MPEventGroup(tag, result);
                            result.groups.set(tag.key, group)
                        }
                        if (t.events === undefined || t.events.payload === undefined || t.events.payload === "") continue;

                        var satelliteMap = new Map();
                        if (t.satelite !== undefined && t.satelite.events !== undefined && t.satelite.events.payload !== undefined && t.events.payload !== "") {
                            var stag = this.tags.get(t.satelite.tag);
                            if (stag !== undefined) {

                                group.satelliteTag = stag;
                                group.displaySettings.set(tp.id, MPDisplaySettings.get(Number(t.satelite.sns), Number(t.satelite.unit)));
                            }

                            for (const s of MPBinaryConverter.dataPointsFromBase64(t.satelite.events.payload)) {
                                satelliteMap.set(s.x.getTime(), s.y);
                            }
                        }
                        for (const e of MPBinaryConverter.dataPointsFromBase64(t.events.payload)) {
                            if (current != null && current.end === null) {
                                current.end = e.x;
                            }

                            if (current === null || current.state !== e.y) {
                                current = new MPEvent(group, e.y, e.x, eq.id, tp.id);
                                group.events.push(current);
                                if (satelliteMap.size > 0) {
                                    var sv = satelliteMap.get(e.x.getTime());
                                    if (sv != undefined) {
                                        current.satelliteValue = sv;
                                    }

                                }
                            }
                        }

                    }

                }

            }
            return result;

        }
    }

    async * getDataSourcesAsync(equipmentId, dataSourceIdCollection = null) {
        var request = new MPIDChildrenJson(equipmentId);
        request.children = dataSourceIdCollection === null ? null : MPIDMultipleJson.create(dataSourceIdCollection).items;
        var response = await this.postJsonAndCheckAsync("/api/datasource", this.request(request));
        if (response != null && response.code === 200) {
            for (const d of response.data) {
                yield new MPDataSource(d);
            }
        }
    }

    async streamUnsubscribeAllAsync() {
        if (this.subscriptions.size > 0) {
            var request = new MPStreamRequest();
            for (const e of this.subscriptions) {
                request.items.push(new MPIDJson(e[0]));
            }

            request.enable = false;
            await this.postJsonAndCheckAsync("/api/stream", this.request(request));
            this.subscriptions = new Map();
        }
    }

    async streamSubscribeAsync(items, tespointSelector = (i) => i, callbackSelector = null, unsubscribeBeforeStart = true) {

        if (unsubscribeBeforeStart)
            await this.streamUnsubscribeAllAsync();

        var enabledTestPoints = items.filter(i => tespointSelector(i).enabled);
        const equipmentGroupKeys = [...new Set(enabledTestPoints.map(i => tespointSelector(i).idEq))]
        const equipmentGroupValues = Object.groupBy(enabledTestPoints, i => tespointSelector(i).idEq);
        var dataSources = new Map();
        for (const e of equipmentGroupKeys) {
            var equipmentGroup = equipmentGroupValues[e];
            const dataSourceIds = [...new Set(equipmentGroup.map(i => tespointSelector(i).idDs))]

            for await (const ds of this.getDataSourcesAsync(e, dataSourceIds)) {

                if (ds.enabled) {
                    var plugin = this.plugins.get(ds.plugin);
                    if (plugin !== undefined && plugin.prps === true)
                        dataSources.set(ds.id, ds);
                }
            }
        }
        var request = new MPStreamRequest();
        for (const e of enabledTestPoints) {
            var dataSource = dataSources.get(tespointSelector(e).idDs);
            if (dataSource !== undefined)
                request.items.push(new MPIDJson(tespointSelector(e).id));
        }

        request.enable = true;
        var testpointIndex = new Map(enabledTestPoints.map(i => [tespointSelector(i).id, i]));

        var response = await this.postJsonAndCheckAsync("/api/stream", this.request(request));
        if (response !== null && response.data.enabled.length > 0) {

            for (const i of response.data.enabled) {
                var tp = testpointIndex.get(i.id);
                if (tp !== undefined) {
                    var subscription = new MPStreamSubscription(tespointSelector(tp));
                    if (callbackSelector != null)
                        subscription.callback = callbackSelector(tp);
                    this.subscriptions.set(tp.id, subscription)

                }
            }
        }
        return this.subscriptions.size > 0 ? true : false;
    }

    async streamPollAsync() {
        var response = await this.postJsonAsync("/api/prps", this.request());

        if (response != null && response.code == 200 && response.data.length > 0) {
            for (const i of response.data) {
                var subscription = this.subscriptions.get(String(i.id));
                if (subscription !== undefined)
                    subscription.onDataReceived(i.frames);
            }
        }
    }

    async getStatisticsAsync(idOrMPIdEquipmentList) {
        var request = new MPStatRequest(false);
        request.addRange(idOrMPIdEquipmentList);
        var response = await this.postJsonAndCheckAsync("/api/stat", this.request(request));
        var result = new MPStatList(response.data);
        return result;
    }

    async * getDatasetIndexAsync(id, from, to) {
        var response = await this.postJsonAndCheckAsync("/api/index", this.request(new MPIndexRequest(null, id, from, to)));
        for (const t of response.data.time)
            yield new Date(t);
    }

    async getComparableDatasetDatesAsync(items, from, to) {
        var dates = new Set();
        for (const i of items) {
            for await (const d of this.getDatasetIndexAsync(i, from, to)) {
                dates.add(d);
            }
        }
        return dates;
    }

    async getLocationResultAsync(request) {
        var response = await this.postJsonAndCheckAsync("/api/location/compare", this.request(request));
        return new MPLocationResult(response.data);
    }

    async * getEquipmentByTestPointAsync(idOrObjList) {
        var response = await this.postJsonAndCheckAsync("/api/equipmentByTestpoint", this.request(MPIDMultipleJson.create(idOrObjList)));
        for await (const id of response.data.items) {
            yield MPIDJson.create(id);
        }
    }

    async getTestPointByIdAsync(idOrObj) {
        var response = await this.postJsonAndCheckAsync("/api/testpointGet", this.request(MPIDJson.create(idOrObj)));
        if (response?.data == null) return null;
        return new MPTestPoint(response.data, null);
    }

    async getSingleAsync(id, from, to, time = null, left = undefined, right = undefined, tag = undefined, ignore = undefined, postProcess = undefined) {
        const idStr = MPIDJson.getId(id);
        var response = await this.postJsonAndCheckAsync("/api/single", this.request(new MPSingleRequest(idStr, from, to, time, left, right, tag, ignore, postProcess)));
        if (response?.data == null) return null;
        return new MPValue(idStr, response.data);
    }

    async getAccumulatedAsync(id, from, to, tag) {
        const idStr = MPIDJson.getId(id);
        var response = await this.postJsonAndCheckAsync("/api/pdAccumulate", this.request(new MPSingleRequest(idStr, from, to, null, undefined, undefined, tag, undefined, undefined)));
        if (response?.data == null) return null;
        return new MPAccumulationResult(response.data, idStr);
    }

    async getPdExpertResultAsync(id, tag, time, getState = false, minX = undefined, maxX = undefined, minY = undefined, maxY = undefined) {
        var response = await this.postJsonAndCheckAsync("/api/pde", this.request(new MPPdeRequest(id, tag, time, this.loginInformation.culture, getState, minX, maxX, minY, maxY)));
        if (response?.data == null) return null;
        return new MPPdeResponse(response.data);
    }

    async getTestPointByKeyAsync(key) {
        var response = await this.postJsonAsync("/api/testpointFind", this.request({ key: String(key) }));
        if (response?.data == null || response.code !== 200) return null;
        return new MPTestPoint(response.data, null);
    }
}

export { MPBinaryConverter, MPTag, MPAmplitudeUnit, MPSensorType, MPDisplaySettings, MPClient, MPLocationCompareRequest, MPFile, ExtendedSVG }
