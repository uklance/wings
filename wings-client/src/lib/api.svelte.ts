import type { StringMap } from './types/StringMap.js'

let nextId: number = 0;
let subscriptionsById: {[key: string]: Subscription} = {};
let socket: WebSocket;
let bufferedCommands: (() => void)[] = [];

let socketState: string = $state('not connected');
let socketSessionId: string | null = $state(null);

export const apiState = {
    get socketState() { return socketState },
    get socketSessionId() { return socketSessionId }
};

export type MessageListener = (message: any) => any;

export class Subscription {
    public entity: string;
    public message:any;
    public id: string;

    messageListeners: MessageListener[] = [];

    constructor(entity: string, message:any, id: string) {
        this.entity = entity;
        this.message = message;
        this.id = id;
    }

    public close() {
        this.messageListeners.length = 0;
        delete subscriptionsById[this.id];
    }

    public onMessage(message: any) {
        this.messageListeners.forEach(listener => listener(message));
    }

    public addMessageListener(listener: MessageListener) {
        this.messageListeners.push(listener);
    }
}

export function subscribe(entity:string, payload:any, headers?:StringMap): Promise<Subscription> {
    let promise = new Promise<Subscription>((resolve, reject) => {
        if (socketSessionId === null) {
            console.log('Buffering subscribe command (socketSessionId is null)');
            bufferedCommands.push(() => resolve(_subscribe(entity, payload, headers)));
        } else {
            resolve(_subscribe(entity, payload, headers));
        }
    });
    return promise;
}

function _subscribe(entity:string, payload:any, headers?:StringMap): Subscription {
    let id: string = String(++nextId).padStart(5, '0');

    let defaultHeaders:StringMap = {
        topic: entity + ':subscribe',
        correlationId: id,
        sessionId: socketSessionId
    };
    let _headers:StringMap = headers ? {...headers, ...defaultHeaders} : defaultHeaders;

    let message = {
        headers: _headers,
        payload: payload
    };

    let subscription = new Subscription(entity, message, id);
    subscriptionsById[id] = subscription;
    post(message);
    return subscription;
}

export function websocketConnect() {
    if (socket) {
        throw new Error('Websocket already connected')
    }
    socket = new WebSocket(`ws://${location.host}/websocket`);

    socket.addEventListener('open', event => {
        console.log('websocket opened');
        socketState = 'open';
        socketSessionId = null;
    });

    socket.addEventListener('close', event => {
        console.log('websocket closed');
        socketState = 'closed';
        socketSessionId = null;
    });  
    
    socket.addEventListener('message', event => {
        console.log(`received message event.data=${event.data}`);
        let message = JSON.parse(event.data);
        if (message.headers.topic === 'Websocket:connect') {
            socketSessionId = message.headers.sessionId;
            console.log(`sessionId is ${socketSessionId}`);
            bufferedCommands.forEach(command => command());
            bufferedCommands.length = 0;
        } else {
            let correlationId:string = message.headers.correlationId;
            let sub:Subscription = subscriptionsById[correlationId];
            if (sub) {
                sub.onMessage(message);
            } else {
                console.log(`No subscriptions for correlationId ${correlationId}`);
            }
        }
    });    
}

async function post(body:any) {
	let opts:any = { 
        method: 'POST', 
        headers: { 'Content-Type' : 'application/json' },
        body: JSON.stringify(body)
    };
    console.log(`sending ${JSON.stringify(opts)}`)
    const res = await fetch(`http://${location.host}/api/event`, opts);
	if (!res.ok) {
        throw new Error(`Received status ${res.status}`);
	}
}