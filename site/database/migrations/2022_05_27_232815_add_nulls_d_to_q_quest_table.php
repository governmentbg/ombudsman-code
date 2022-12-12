<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddNullsDToQQuestTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('q_quest', function (Blueprint $table) {
            $table->string('Q_desc', 1000)->nullable()->change();
            $table->date('Q_date_from')->nullable()->change();
            $table->date('Q_date_to')->nullable()->change();
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('q_quest', function (Blueprint $table) {
            //
        });
    }
}
