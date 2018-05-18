/*
 * Copyright 2017 Lime - HighTech Solutions s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import React from "react";
import {connect} from "react-redux";
// Components
import Spinner from 'react-tiny-spin';
import {FormGroup} from "react-bootstrap";
// Custom react-select component for bank account choice with HTML content
import BankAccountSelect from "./bankAccountSelect";
// Actions
import {updateFormData} from "../actions/operationDetailActions";
// i18n
import {FormattedMessage} from "react-intl";

/**
 * Operation details which can be embedded in other components.
 * Operation data should be available in store via context.formData or context.data.
 */
@connect((store) => {
    return {
        context: store.dispatching.context
    }
})
export default class OperationDetail extends React.Component {

    constructor() {
        super();
        this.initBankAccounts = this.initBankAccounts.bind(this);
        this.findChosenBankAccount = this.findChosenBankAccount.bind(this);
        this.updateChosenBankAccount = this.updateChosenBankAccount.bind(this);
        this.handleBankAccountChoice = this.handleBankAccountChoice.bind(this);
        this.storeBankAccounts = this.storeBankAccounts.bind(this);
        this.setBankAccountChoiceDisabled = this.setBankAccountChoiceDisabled.bind(this);
        this.state = {bankAccounts: null, chosenBankAccount: null, bankAccountChoiceDisabled: false};
    }

    componentWillMount() {
        this.initBankAccounts(this.props);
    }

    componentWillReceiveProps(props) {
        this.initBankAccounts(props);
    }

    initBankAccounts(props) {
        if (props.context.formData) {
            props.context.formData.parameters.map((item) => {
                if (item.type === "BANK_ACCOUNT_CHOICE") {
                    // save bank accounts for easier switching of bank accounts
                    this.storeBankAccounts(item.bankAccounts);
                    if (!item.enabled) {
                        // when item is disabled on backend, set choice to always disabled
                        this.setBankAccountChoiceDisabled(true);
                    }
                    if (props.context.formData.userInput["operation.bankAccountChoice.disabled"]) {
                        // bank account has already been chosen
                        this.updateChosenBankAccount(item.bankAccounts, props.context.formData.userInput["operation.bankAccountChoice"]);
                        // freeze choice for item
                        this.setBankAccountChoiceDisabled(true);
                    } else if (this.props.context.formData.userInput["operation.bankAccountChoice"]) {
                        // bank account has already been chosen, set the chosen value
                        this.updateChosenBankAccount(item.bankAccounts, props.context.formData.userInput["operation.bankAccountChoice"]);
                    } else {
                        // initial state - no value has been chosen yet
                        let defaultBankAccount;
                        if (item.defaultValue) {
                            // when default value is set by backend, use it
                            defaultBankAccount = this.findChosenBankAccount(item.bankAccounts, item.defaultValue);
                            if (defaultBankAccount === undefined) {
                                // default value was not found, use the first bank account
                                defaultBankAccount = item.bankAccounts[0];
                            }
                        } else {
                            // otherwise initial bank account is set to the first bank account
                            defaultBankAccount = item.bankAccounts[0];
                        }
                        this.handleBankAccountChoice(defaultBankAccount);
                    }
                }
            });
        }
    }

    storeBankAccounts(bankAccountsReceived) {
        // bankAccount are saved for easier work with bank accounts
        this.setState({bankAccounts: bankAccountsReceived});
    }

    findChosenBankAccount(bankAccounts, chosenBankAccountId) {
        let chosenBankAccount = undefined;
        bankAccounts.map((bankAccount) => {
            if (bankAccount.accountId === chosenBankAccountId) {
                chosenBankAccount = bankAccount;
            }
        });
        return chosenBankAccount;
    }

    updateChosenBankAccount(bankAccounts, chosenBankAccountId) {
        const bankAccount = this.findChosenBankAccount(bankAccounts, chosenBankAccountId);
        this.setState({chosenBankAccount: bankAccount});
    }

    handleBankAccountChoice(bankAccount) {
        this.setState({chosenBankAccount: bankAccount});
        this.props.context.formData.userInput["operation.bankAccountChoice"] = bankAccount.accountId;
        this.props.dispatch(updateFormData(this.props.context.formData));
    }

    setBankAccountChoiceDisabled(disabled) {
        this.setState({bankAccountChoiceDisabled: disabled});
    }

    render() {
        if (this.props.context.formData) {
            return (
                <div>
                    <div>
                        {this.props.context.formData.banners ?
                            this.props.context.formData.banners.map((banner) => {
                                return this.displayBanner(banner, false);
                            }) : (
                                undefined
                            )}
                    </div>
                    <div className="operation-approve content-wrap">
                        <h3 className="title">{this.props.context.formData.title.message}</h3>
                        <p>{this.props.context.formData.greeting.message}</p>
                    </div>
                    <div>
                        {this.props.context.formData.parameters.map((item) => {
                            if (item.type === "AMOUNT") {
                                return this.displayAmount(item);
                            } else if (item.type === "KEY_VALUE") {
                                return this.displayKeyValue(item);
                            } else if (item.type === "NOTE") {
                                return this.displayNote(item);
                            } else if (item.type === "HEADING") {
                                return this.displayHeading(item);
                            } else if (item.type === "BANK_ACCOUNT_CHOICE") {
                                return this.displayBankAccountChoice(item);
                            } else if (item.type === "BANNER") {
                                return this.displayBanner(item, true);
                            } else if (item.type === "PARTY_INFO") {
                                return this.displayPartyInfo(item);
                            }
                        })}
                    </div>
                </div>
            )
        } else if (this.props.context.data) {
            return (
                <div>
                    <FormGroup>
                        DATA: {this.props.context.data}
                    </FormGroup>
                </div>
            )
        } else {
            return (
                <Spinner/>
            )
        }
    }

    displayAmount(amount) {
        return (
            <div className="row attribute" key={amount.id}>
                <div className="col-xs-6 key">
                    {amount.label}
                </div>
                <div className="col-xs-6 value">
                    <span className="amount">{amount.formattedValue}</span>
                </div>
            </div>
        )
    }

    displayKeyValue(keyValue) {
        return (
            <div className="row attribute" key={keyValue.id}>
                <div className="col-xs-6 key">
                    {keyValue.label}
                </div>
                <div className="col-xs-6 value">
                    {keyValue.formattedValue}
                </div>
            </div>
        )
    }

    displayNote(note) {
        return (
            <div className="row attribute" key={note.id}>
                <div className="col-xs-12">
                    <div className="key">{note.label}</div>
                    <div className="value">{note.formattedValue}</div>
                </div>
            </div>
        )
    }

    displayHeading(heading) {
        return (
            <div className="row attribute" key={heading.id}>
                <div className="col-xs-12 heading">{heading.formattedValue}</div>
            </div>
        )
    }

    displayBankAccountChoice(bankAccountChoice) {
        if (!bankAccountChoice.bankAccounts || bankAccountChoice.bankAccounts.length === 0) {
            // no bank account is available - display error
            return (
                <div className={'message-error'} key={bankAccountChoice.id}>
                    <FormattedMessage id="operationReview.bankAccountsMissing"/>
                </div>
            )
        } else {
            return (
                <div key={bankAccountChoice.id} className="row attribute">
                    <div className="col-xs-12">
                        <div className="key">
                            {bankAccountChoice.label}
                        </div>
                        <div className="value">
                            {(this.state.bankAccounts && this.state.chosenBankAccount) ? (
                                <BankAccountSelect
                                    bankAccounts={this.state.bankAccounts}
                                    chosenBankAccount={this.state.chosenBankAccount}
                                    choiceDisabled={this.state.bankAccountChoiceDisabled}
                                    callback={this.handleBankAccountChoice}
                                />
                            ) : (
                                undefined
                            )}
                        </div>
                    </div>
                </div>
            )
        }
    }

    displayBanner(banner, isFieldBanner) {
        let bannerClass = "alert";
        if (isFieldBanner) {
            bannerClass+= " alert-field";
        } else {
            bannerClass+= " alert-form";
        }
        let glyphIconClass = "";
        switch (banner.bannerType) {
            case "BANNER_INFO":
                bannerClass += " alert-info";
                glyphIconClass = "glyphicon glyphicon-info-sign";
                break;
            case "BANNER_WARNING":
                bannerClass += " alert-warning";
                glyphIconClass = "glyphicon glyphicon-warning-sign";
                break;
            case "BANNER_ERROR":
                bannerClass += " alert-danger";
                glyphIconClass = "glyphicon glyphicon-exclamation-sign";
                break;
        }
        return (
            <div className={bannerClass} key={banner.id}>
                <span className={glyphIconClass}/>
                &nbsp;&nbsp;
                {banner.message}
            </div>
        );
    }

    displayPartyInfo(info) {
        const partyInfo = info.partyInfo;
        return (
            <div className="row attribute" key={info.id}>
                <div className="col-xs-6 key">
                    {info.label}
                    <br/>
                    <span className="party-info-name">{partyInfo.name}</span>
                </div>
                <div className="col-xs-6 value">
                    <img src={partyInfo.logoUrl} className="party-info-logo"/>
                    <br/>
                    <span className="party-info-description">{partyInfo.description}</span>
                    <br/>
                    <a href={partyInfo.websiteUrl} target="_blank" className="party-info-link">
                        <FormattedMessage id="partyInfo.websiteLink"/>
                    </a>
                </div>
            </div>
        )
    }
}

