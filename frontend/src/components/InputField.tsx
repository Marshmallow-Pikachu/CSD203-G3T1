import React, { type FunctionComponent } from "react";
import {input} from "react-dom";


export default function InputField({label, name, type}: {label: string, name: string, type: string}) {
    return (
        <div className="input-container">
            <p className="input-label">{label}</p>
            <input name={name} type={type} />
        </div>
    )
}