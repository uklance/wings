import type { StringMap } from './types/StringMap.js'
import { error } from '@sveltejs/kit';

let nextId: number = 0;
let subscriptionsById: {[key: string]: Subscription} = {};
let socket: WebSocket;

let socketState: string = $state('not connected');
let socketSessionId: string | null = $state(null);

export const apiState = {
    get socketState() { return socketState },
    get socketSessionId() { return socketSessionId }
}

export type MessageListener = (message: any) => any;

export class Subscription {
    public modelName: string;
    public message:any;
    public id: string;

    messageListeners: MessageListener[] = [];

    constructor(modelName: string, message:any, id: string) {
        this.modelName = modelName;
        this.message = message;
        this.id = id;
    }

    public close() {
        this.messageListeners.length = 0;
        delete subscriptionsById[this.id]
    }

    public onMessage(message: any) {
        this.messageListeners.forEach(listener => listener(message));
    }

    public addMessageListener(listener: MessageListener) {
        this.messageListeners.push(listener);
    }
}

export function subscribe(modelName:string, payload:any, headers?:StringMap): Subscription {
    let id: string = String(++nextId).padStart(5);

    if (!socketSessionId) {
        error(500, { message: "socketSessionId is null"})
    }

    let defaultHeaders:StringMap = {
        topic: modelName + ':subscribe',
        correlationId: id,
        sessionId: socketSessionId!
    };
    let _headers:StringMap = headers ? {...headers, ...defaultHeaders} : defaultHeaders;

    let message = {
        headers: _headers,
        payload: payload
    };

    let subscription = new Subscription(modelName, message, id);
    subscriptionsById[id] = subscription;
    return subscription;
}

export function websocketConnect() {
    if (socket) {
        error(500, { message: "Websocket already connected"})
    }
    socket = new WebSocket(`ws://${location.host}/websocket`);

    socket.addEventListener('open', event => {
        console.log('websocket opened')
        socketState = 'open'
        socketSessionId = null
    });

    socket.addEventListener('close', event => {
        console.log('websocket closed')
        socketState = 'closed'
        socketSessionId = null
    });  
    
    socket.addEventListener('message', event => {
        let message = JSON.parse(event.data)
        if (message.headers.topic == 'Websocket:init') {
            console.log(`sessionId is ${message.payload.sessionId}`)
            socketSessionId = message.payload.sessionId
        } else {
            for (let id in subscriptionsById) {
                subscriptionsById[id].onMessage(message);
            }
            /*
            let correlationIds: string[] | null = message.headers.correlationIds;
            if (!correlationIds) {
                error(500, { message: "message.headers.correlationIds is null"})
            }
            correlationIds?.forEach( id => {
                subscriptionsById[id]?.onMessage(message);
            })*/

        }
    })    
}