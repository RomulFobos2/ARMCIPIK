import { IEventSystem } from "../../ts-common/events";
import { Layout } from "../../ts-layout";
export interface ITimepickerConfig {
    css?: string;
    timeFormat?: 12 | 24;
    controls?: boolean;
    value?: Date | number | string | any[] | ITimeObject;
    valueFormat?: "string" | "timeObject";
    /** @deprecated See a documentation: https://docs.dhtmlx.com/ */
    actions?: boolean;
}
export interface ITimepicker {
    config: ITimepickerConfig;
    events: IEventSystem<TimepickerEvents, ITimepickerHandlersMap>;
    layout: Layout;
    paint(): void;
    getValue(asOBject?: boolean): ITimeObject | string;
    setValue(value: Date | number | string | any[] | ITimeObject): void;
    clear(): void;
    destructor(): void;
}
export interface ITimeObject {
    hour: number;
    minute: number;
    AM?: boolean;
}
export declare enum TimepickerEvents {
    change = "change",
    beforeApply = "beforeApply",
    afterApply = "afterApply",
    beforeClose = "beforeClose",
    afterClose = "afterClose",
    /** @deprecated See a documentation: https://docs.dhtmlx.com/ */
    apply = "apply",
    /** @deprecated See a documentation: https://docs.dhtmlx.com/ */
    close = "close",
    /** @deprecated See a documentation: https://docs.dhtmlx.com/ */
    save = "save"
}
export interface ITimepickerHandlersMap {
    [key: string]: (...args: any[]) => any;
    [TimepickerEvents.change]: (time: string | ITimeObject) => void;
    [TimepickerEvents.beforeApply]: (time: string | ITimeObject) => void;
    [TimepickerEvents.afterApply]: (time: string | ITimeObject) => boolean | void;
    [TimepickerEvents.beforeClose]: (time: string | ITimeObject) => boolean | void;
    [TimepickerEvents.afterClose]: (time: string | ITimeObject) => void;
    [TimepickerEvents.apply]: (time: string) => void;
    [TimepickerEvents.close]: () => any;
    [TimepickerEvents.save]: (time?: ITimeObject) => any;
}
