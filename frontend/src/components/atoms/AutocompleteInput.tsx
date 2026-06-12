import { useId, useState, useEffect } from "react";

export interface AutocompleteOption {
    id: string;
    label: string;
}

export interface AutocompleteInputProps {
    options: AutocompleteOption[];
    value: string;
    onChange: (value: string) => void;
    placeholder?: string;
    className?: string;
    disabled?: boolean;
    allowFreeText?: boolean;
}

export function AutocompleteInput({
    options,
    value,
    onChange,
    placeholder = "Tìm kiếm...",
    className = "",
    disabled = false,
    allowFreeText = false,
}: AutocompleteInputProps) {
    const listId = useId();
    const [inputValue, setInputValue] = useState("");

    useEffect(() => {
        const option = options.find((opt) => opt.id === value);
        if (option) {
            setInputValue(option.label);
        } else if (!value) {
            setInputValue("");
        }
    }, [value, options]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const newLabel = e.target.value;
        setInputValue(newLabel);

        const matchedOption = options.find((opt) => opt.label === newLabel);
        if (matchedOption) {
            onChange(matchedOption.id);
        } else {
            onChange(allowFreeText ? newLabel : "");
        }
    };

    const filteredOptions = (options || [])
        .filter((opt) => {
            const lbl = opt && opt.label ? String(opt.label).toLowerCase() : "";
            const val = inputValue ? String(inputValue).toLowerCase() : "";
            return lbl.includes(val);
        })
        .slice(0, 20);

    return (
        <div className="relative w-full">
            <input
                role="combobox"
                type="text"
                list={listId}
                value={inputValue}
                onChange={handleChange}
                placeholder={placeholder}
                disabled={disabled}
                className={`h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-[14px] text-slate-900 outline-none placeholder:text-slate-400 disabled:bg-slate-50 disabled:text-slate-500 ${className}`}
            />
            <datalist id={listId}>
                {filteredOptions.map((opt, index) => (
                    <option key={`${opt.id}-${index}`} value={opt.label} />
                ))}
            </datalist>
        </div>
    );
}
